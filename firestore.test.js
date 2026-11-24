const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTs(db) {
  return db.app.firebase.firestore.FieldValue.serverTimestamp();
}

function validWallet(db, userId = ALICE_UID) {
  return {
    userId,
    name: "BCA",
    walletType: "Bank",
    balance: 5000000,
    accentHex: 0xff1a1a1a,
    accountNumberHint: "•••• 4829",
    clientUpdatedAt: Date.now(),
    createdAt: serverTs(db),
    updatedAt: serverTs(db),
  };
}

function validTransaction(db, userId = ALICE_UID, walletSyncId = "wallet-uuid-1") {
  return {
    userId,
    merchant: "Kopi Kenangan",
    amount: 25000,
    type: "EXPENSE",
    category: "Kopi & Jajan",
    walletSyncId,
    note: "Kopi pagi",
    timestamp: Date.now(),
    isAiGenerated: false,
    clientUpdatedAt: Date.now(),
    createdAt: serverTs(db),
    updatedAt: serverTs(db),
  };
}

function validBudget(db, userId = ALICE_UID) {
  return {
    userId,
    category: "Kopi & Jajan",
    monthlyLimit: 300000,
    monthKey: "2026-11",
    clientUpdatedAt: Date.now(),
    createdAt: serverTs(db),
    updatedAt: serverTs(db),
  };
}

function validRecurring(db, userId = ALICE_UID, walletSyncId = "wallet-uuid-1") {
  return {
    userId,
    merchant: "Kos & Internet",
    amount: 1850000,
    type: "EXPENSE",
    category: "Tagihan & Kos",
    walletSyncId,
    note: "Bulanan",
    frequency: "MONTHLY",
    startDate: Date.now(),
    nextDueDate: Date.now() + 86400000,
    isActive: true,
    clientUpdatedAt: Date.now(),
    createdAt: serverTs(db),
    updatedAt: serverTs(db),
  };
}

function validSavingsGoal(db, userId = ALICE_UID) {
  return {
    userId,
    title: "Dana Darurat",
    targetAmount: 10000000,
    savedAmount: 2500000,
    accentHex: 0xffb5ee5a,
    iconName: "SHIELD",
    note: "Target 6 bulan",
    completed: false,
    clientUpdatedAt: Date.now(),
    createdAt: serverTs(db),
    updatedAt: serverTs(db),
  };
}

function validDebt(db, userId = ALICE_UID) {
  return {
    userId,
    personName: "Rama",
    amount: 350000,
    paidAmount: 100000,
    direction: "LEND",
    note: "Talangan",
    settled: false,
    createdAtMillis: Date.now(),
    clientUpdatedAt: Date.now(),
    createdAt: serverTs(db),
    updatedAt: serverTs(db),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot list or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("wallets").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("recurring_transactions")
      .get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("savings_goals").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("debts").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("wallets")
      .doc("wallet-1")
      .set(validWallet(unauthDb))
  );
});

test("Authenticated owner can create, list, update, and delete wallets, transactions, budgets, and recurring", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRoot = aliceDb.collection("users").doc(ALICE_UID);

  // Create
  const walletRef = aliceRoot.collection("wallets").doc("wallet-uuid-1");
  await assertSucceeds(walletRef.set(validWallet(aliceDb)));
  const txRef = aliceRoot.collection("transactions").doc("tx-uuid-1");
  await assertSucceeds(txRef.set(validTransaction(aliceDb)));
  const budgetRef = aliceRoot.collection("budgets").doc("budget-uuid-1");
  await assertSucceeds(budgetRef.set(validBudget(aliceDb)));
  const recRef = aliceRoot
    .collection("recurring_transactions")
    .doc("rec-uuid-1");
  await assertSucceeds(recRef.set(validRecurring(aliceDb)));

  // List
  await assertSucceeds(aliceRoot.collection("wallets").get());
  await assertSucceeds(aliceRoot.collection("transactions").get());
  await assertSucceeds(aliceRoot.collection("budgets").get());
  await assertSucceeds(aliceRoot.collection("recurring_transactions").get());

  // Update preserving createdAt
  const walletSnap = await walletRef.get();
  const existingCreatedAt = walletSnap.data().createdAt;
  await assertSucceeds(
    walletRef.set({
      ...validWallet(aliceDb),
      balance: 4975000,
      createdAt: existingCreatedAt,
      updatedAt: serverTs(aliceDb),
    })
  );

  // Delete
  await assertSucceeds(txRef.delete());

  // Savings Goals & Debts CRUD + update preserving createdAt
  const goalRef = aliceRoot.collection("savings_goals").doc("goal-uuid-1");
  await assertSucceeds(goalRef.set(validSavingsGoal(aliceDb)));
  await assertSucceeds(aliceRoot.collection("savings_goals").get());
  const goalCreatedAt = (await goalRef.get()).data().createdAt;
  await assertSucceeds(
    goalRef.set({
      ...validSavingsGoal(aliceDb),
      savedAmount: 10000000,
      completed: true,
      createdAt: goalCreatedAt,
      updatedAt: serverTs(aliceDb),
    })
  );
  await assertSucceeds(goalRef.delete());

  const debtRef = aliceRoot.collection("debts").doc("debt-uuid-1");
  await assertSucceeds(debtRef.set(validDebt(aliceDb)));
  await assertSucceeds(aliceRoot.collection("debts").get());
  const debtCreatedAt = (await debtRef.get()).data().createdAt;
  await assertSucceeds(
    debtRef.set({
      ...validDebt(aliceDb),
      paidAmount: 350000,
      settled: true,
      createdAt: debtCreatedAt,
      updatedAt: serverTs(aliceDb),
    })
  );
  await assertSucceeds(debtRef.delete());
});

test("Cross-user access is strictly denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx-alice-1")
      .set(validTransaction(aliceDb, ALICE_UID))
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx-alice-1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx-bob-spoof")
      .set(validTransaction(bobDb, BOB_UID))
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx-alice-1")
      .delete()
  );
});

test("Schema validation blocks ghost fields, invalid amounts, bad monthKey, and createdAt tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRoot = aliceDb.collection("users").doc(ALICE_UID);

  // Ghost field rejected
  await assertFails(
    aliceRoot
      .collection("wallets")
      .doc("wallet-ghost")
      .set({ ...validWallet(aliceDb), isAdmin: true })
  );

  // Zero or negative transaction amount rejected
  await assertFails(
    aliceRoot
      .collection("transactions")
      .doc("tx-zero")
      .set({ ...validTransaction(aliceDb), amount: 0 })
  );

  // Bad monthKey rejected
  await assertFails(
    aliceRoot
      .collection("budgets")
      .doc("budget-bad-month")
      .set({ ...validBudget(aliceDb), monthKey: "2026/11" })
  );

  // Immutable createdAt tampering rejected on update
  const txRef = aliceRoot.collection("transactions").doc("tx-immutable");
  await assertSucceeds(txRef.set(validTransaction(aliceDb)));
  await assertFails(
    txRef.set({
      ...validTransaction(aliceDb),
      amount: 30000,
      createdAt: serverTs(aliceDb),
      updatedAt: serverTs(aliceDb),
    })
  );

  // Debt paidAmount exceeding amount rejected
  await assertFails(
    aliceRoot
      .collection("debts")
      .doc("debt-overpaid")
      .set({ ...validDebt(aliceDb), amount: 100000, paidAmount: 150000 })
  );
});
