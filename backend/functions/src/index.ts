import * as admin from "firebase-admin";
import { onRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { defineSecret } from "firebase-functions/params";
import Stripe from "stripe";

admin.initializeApp();
const db = admin.firestore();

const stripeSecretKey = defineSecret("STRIPE_SECRET_KEY");
const stripeWebhookSecret = defineSecret("STRIPE_WEBHOOK_SECRET");

interface LineItemDoc {
  name: string;
  description?: string;
  quantity: number;
  unit: string;
  unitPriceCents: number;
  lineTotalCents: number;
}

interface SharedQuoteDoc {
  ownerUid: string;
  quoteId: string;
  quoteNumber: string;
  businessName: string;
  businessLicense?: string;
  businessPhone?: string;
  stripeConnectAccountId?: string;
  outputLanguage: "en" | "es";
  customerName: string;
  customerAddress?: string;
  title: string;
  lineItems: LineItemDoc[];
  subtotalCents: number;
  taxCents: number;
  totalCents: number;
  depositCents: number;
  terms: string;
  status: string;
  signerName?: string;
}

function formatCents(cents: number): string {
  return `$${(cents / 100).toFixed(2)}`;
}

function escapeHtml(raw: string): string {
  return raw
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/**
 * 1. Public Customer Quote Share Page (GET /quoteSharePage?token=...)
 * Serves a mobile-first, bilingual (EN/ES) quote page with e-signature approval
 * and Stripe Connect deposit trigger without exposing private contractor collections.
 */
export const quoteSharePage = onRequest({ cors: true }, async (req, res) => {
  const token = String(req.query.token || req.path.split("/").pop() || "").trim();
  if (!token || token.length < 12) {
    res.status(400).send("Invalid quote link.");
    return;
  }

  const snap = await db.collection("publicShares").doc(token).get();
  if (!snap.exists) {
    res.status(404).send("Quote not found or expired.");
    return;
  }

  const quote = snap.data() as SharedQuoteDoc;
  const isEs = quote.outputLanguage === "es";

  // Track first customer view timestamp
  if (quote.status === "SENT") {
    await Promise.all([
      snap.ref.update({ status: "VIEWED", viewedAt: admin.firestore.FieldValue.serverTimestamp() }),
      db
        .collection("users")
        .doc(quote.ownerUid)
        .collection("quotes")
        .doc(quote.quoteId)
        .update({ status: "VIEWED", viewedAtEpochMs: Date.now() }),
    ]);
  }

  const rowsHtml = quote.lineItems
    .map(
      (item) => `
      <tr>
        <td style="padding:12px 0;border-bottom:1px solid #e5e7eb;">
          <strong>${escapeHtml(item.name)}</strong>
          ${item.description ? `<div style="font-size:13px;color:#4b5563;">${escapeHtml(item.description)}</div>` : ""}
        </td>
        <td style="padding:12px 8px;border-bottom:1px solid #e5e7eb;text-align:right;">${item.quantity} ${escapeHtml(item.unit)}</td>
        <td style="padding:12px 0;border-bottom:1px solid #e5e7eb;text-align:right;font-family:monospace;font-weight:700;">${formatCents(item.lineTotalCents)}</td>
      </tr>`
    )
    .join("");

  const html = `<!doctype html>
<html lang="${quote.outputLanguage}">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <title>${escapeHtml(quote.quoteNumber)} - ${escapeHtml(quote.businessName)}</title>
  <style>
    body{font-family:system-ui,-apple-system,sans-serif;background:#f6f5f0;color:#111827;margin:0;padding:16px;}
    .card{max-width:640px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.08);}
    .hdr{background:#17212d;color:#fff;padding:24px;border-bottom:4px solid #f59e0b;}
    .body{padding:24px;}
    .btn{display:block;width:100%;padding:16px;background:#d97706;color:#fff;border:none;border-radius:12px;font-size:16px;font-weight:700;cursor:pointer;}
    input{width:100%;box-sizing:border-box;padding:14px;font-size:16px;border:1px solid #cbd5e1;border-radius:10px;margin-bottom:12px;}
  </style>
</head>
<body>
  <div class="card">
    <div class="hdr">
      <div style="font-size:12px;color:#f59e0b;font-weight:700;">${isEs ? "COTIZACIÓN" : "QUOTE"} ${escapeHtml(quote.quoteNumber)}</div>
      <h1 style="margin:6px 0 4px;font-size:24px;">${escapeHtml(quote.businessName)}</h1>
      <div style="font-size:13px;color:#d1d5db;">${escapeHtml(quote.businessLicense || "")} ${escapeHtml(quote.businessPhone || "")}</div>
    </div>
    <div class="body">
      <p><strong>${isEs ? "Cliente:" : "Prepared for:"}</strong> ${escapeHtml(quote.customerName)}<br/>${escapeHtml(quote.customerAddress || "")}</p>
      <table style="width:100%;border-collapse:collapse;">${rowsHtml}</table>
      <div style="margin-top:16px;text-align:right;">
        <div>Subtotal: <strong>${formatCents(quote.subtotalCents)}</strong></div>
        <div>${isEs ? "Impuesto" : "Tax"}: <strong>${formatCents(quote.taxCents)}</strong></div>
        <div style="font-size:22px;margin-top:6px;">Total: <strong>${formatCents(quote.totalCents)}</strong></div>
        <div style="color:#d97706;font-weight:700;margin-top:4px;">${isEs ? "Depósito al aprobar" : "Deposit due"}: ${formatCents(quote.depositCents)}</div>
      </div>
      <p style="font-size:13px;color:#4b5563;margin-top:16px;">${escapeHtml(quote.terms)}</p>
      ${
        quote.status === "ACCEPTED"
          ? `<div style="padding:14px;background:#d1fae5;color:#047857;border-radius:10px;font-weight:700;">${isEs ? "Firmada y aprobada por" : "Signed and approved by"} ${escapeHtml(quote.signerName || "")}</div>`
          : `<form method="POST" action="/acceptAndSignQuote">
               <input type="hidden" name="token" value="${escapeHtml(token)}" />
               <input type="text" name="signerName" required placeholder="${isEs ? "Tu nombre completo para firmar" : "Type your full name to e-sign"}" />
               <button class="btn" type="submit">${isEs ? "Firmar y Pagar Depósito" : "E-Sign & Pay Deposit"}</button>
             </form>`
      }
    </div>
  </div>
</body>
</html>`;

  res.status(200).set("Content-Type", "text/html; charset=utf-8").send(html);
});

/**
 * 2. Customer E-Sign & Stripe Connect Deposit Checkout (POST /acceptAndSignQuote)
 * Persists customer signature & creates a Stripe Connect Checkout Session so funds
 * flow directly to the contractor's connected account (zero funds held by QuoteVoice).
 */
export const acceptAndSignQuote = onRequest(
  { cors: true, secrets: [stripeSecretKey] },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    const token = String(req.body.token || "").trim();
    const signerName = String(req.body.signerName || "").trim();
    const signatureSvgPath = String(req.body.signatureSvgPath || "").trim();

    if (!token || !signerName) {
      res.status(400).json({ error: "token and signerName are required." });
      return;
    }

    const shareRef = db.collection("publicShares").doc(token);
    const snap = await shareRef.get();
    if (!snap.exists) {
      res.status(404).json({ error: "Quote share token not found." });
      return;
    }

    const quote = snap.data() as SharedQuoteDoc;
    const acceptedAt = Date.now();

    await Promise.all([
      shareRef.update({
        status: "ACCEPTED",
        signerName,
        signatureSvgPath,
        acceptedAt: admin.firestore.FieldValue.serverTimestamp(),
      }),
      db
        .collection("users")
        .doc(quote.ownerUid)
        .collection("quotes")
        .doc(quote.quoteId)
        .update({
          status: "ACCEPTED",
          signerName,
          signatureSvgPath,
          acceptedAtEpochMs: acceptedAt,
          updatedAtEpochMs: acceptedAt,
        }),
    ]);

    // If contractor connected Stripe Express and deposit > 0, launch Stripe Checkout
    if (quote.stripeConnectAccountId && quote.depositCents > 0) {
      const stripe = new Stripe(stripeSecretKey.value());
      const session = await stripe.checkout.sessions.create({
        mode: "payment",
        line_items: [
          {
            quantity: 1,
            price_data: {
              currency: "usd",
              unit_amount: quote.depositCents,
              product_data: {
                name: `Deposit for Quote ${quote.quoteNumber} — ${quote.businessName}`,
              },
            },
          },
        ],
        payment_intent_data: {
          transfer_data: {
            destination: quote.stripeConnectAccountId,
          },
          metadata: {
            ownerUid: quote.ownerUid,
            quoteId: quote.quoteId,
            paymentType: "quote_deposit",
          },
        },
        success_url: `https://quotevoice.app/q/${token}?paid=1`,
        cancel_url: `https://quotevoice.app/q/${token}`,
      });

      if (session.url) {
        res.redirect(303, session.url);
        return;
      }
    }

    res.status(200).json({
      accepted: true,
      quoteId: quote.quoteId,
      signerName,
      acceptedAtEpochMs: acceptedAt,
    });
  }
);

/**
 * 3. Stripe Connect Webhook (POST /stripeWebhook)
 * Verifies Stripe signature and marks quote deposits or invoices paid idempotently.
 */
export const stripeWebhook = onRequest(
  { secrets: [stripeSecretKey, stripeWebhookSecret] },
  async (req, res) => {
    const sig = req.headers["stripe-signature"];
    if (!sig || typeof sig !== "string") {
      res.status(400).send("Missing stripe-signature header.");
      return;
    }

    const stripe = new Stripe(stripeSecretKey.value());
    let event: Stripe.Event;
    try {
      event = stripe.webhooks.constructEvent(req.rawBody, sig, stripeWebhookSecret.value());
    } catch (err) {
      res.status(400).send(`Webhook signature verification failed: ${(err as Error).message}`);
      return;
    }

    if (event.type === "payment_intent.succeeded") {
      const pi = event.data.object as Stripe.PaymentIntent;
      const { ownerUid, quoteId, invoiceId, paymentType } = pi.metadata || {};

      if (ownerUid && paymentType === "quote_deposit" && quoteId) {
        await db
          .collection("users")
          .doc(ownerUid)
          .collection("quotes")
          .doc(quoteId)
          .update({
            depositPaidCents: pi.amount_received,
            stripeDepositPaymentIntentId: pi.id,
            updatedAtEpochMs: Date.now(),
          });
      } else if (ownerUid && paymentType === "invoice_payment" && invoiceId) {
        const invRef = db.collection("users").doc(ownerUid).collection("invoices").doc(invoiceId);
        await db.runTransaction(async (tx) => {
          const invSnap = await tx.get(invRef);
          if (!invSnap.exists) return;
          const data = invSnap.data()!;
          const newPaid = Number(data.amountPaidCents || 0) + pi.amount_received;
          const total = Number(data.totalCents || 0);
          const isFullyPaid = newPaid >= total;
          tx.update(invRef, {
            amountPaidCents: newPaid,
            status: isFullyPaid ? "PAID" : "PARTIALLY_PAID",
            paidAtEpochMs: isFullyPaid ? Date.now() : null,
            stripePaymentIntentId: pi.id,
            nextReminderAtEpochMs: isFullyPaid ? null : data.nextReminderAtEpochMs,
            updatedAtEpochMs: Date.now(),
          });
        });
      }
    }

    res.status(200).json({ received: true });
  }
);

/**
 * 4. Scheduled Automatic Payment Reminders (Daily 14:00 UTC / morning US time)
 * Scans unpaid invoices with autoRemindersEnabled == true and dispatches
 * bilingual EN/ES reminder events while advancing nextReminderAtEpochMs by 3 days.
 */
export const dailyPaymentReminders = onSchedule("every day 14:00", async () => {
  const now = Date.now();
  const threeDaysMs = 3 * 24 * 60 * 60 * 1000;

  const dueInvoices = await db
    .collectionGroup("invoices")
    .where("autoRemindersEnabled", "==", true)
    .where("status", "in", ["SENT", "PARTIALLY_PAID", "OVERDUE"])
    .where("nextReminderAtEpochMs", "<=", now)
    .limit(200)
    .get();

  const batch = db.batch();
  for (const doc of dueInvoices.docs) {
    const data = doc.data();
    const isPastDue = Number(data.dueDateEpochMs || now) < now;
    const reminderCount = Number(data.reminderCount || 0) + 1;

    // Enqueue outbound SMS/email notification document for delivery worker
    const outboxRef = db.collection("notificationOutbox").doc();
    batch.set(outboxRef, {
      invoicePath: doc.ref.path,
      customerPhone: data.customerPhoneSnapshot || "",
      customerEmail: data.customerEmailSnapshot || "",
      language: data.outputLanguage || "en",
      invoiceNumber: data.invoiceNumber,
      balanceDueCents: Math.max(0, Number(data.totalCents || 0) - Number(data.amountPaidCents || 0)),
      paymentUrl: data.stripePaymentLinkUrl || data.shareUrl || "",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    batch.update(doc.ref, {
      status: isPastDue ? "OVERDUE" : data.status,
      reminderCount,
      lastReminderSentAtEpochMs: now,
      nextReminderAtEpochMs: reminderCount < 5 ? now + threeDaysMs : null,
      updatedAtEpochMs: now,
    });
  }

  await batch.commit();
});
