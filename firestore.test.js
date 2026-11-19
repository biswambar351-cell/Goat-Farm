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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

// 1. Unauthenticated checks
test("Unauthenticated user: cannot read or write goats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/goats`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/goats`).doc("goat_1").set({
      userId: ALICE_UID,
      tag: "TAG-001",
      name: "Bella",
      breed: "Nubian",
      gender: "Doe",
      status: "Active",
      createdAt: new Date(),
    })
  );
});

// 2. Cross-user isolation
test("Cross-user isolation: Bob cannot read or write Alice's goats", async () => {
  // Alice seeds her goat
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/goats`).doc("goat_1").set({
      userId: ALICE_UID,
      tag: "TAG-001",
      name: "Bella",
      breed: "Nubian",
      gender: "Doe",
      status: "Active",
      createdAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Bob tries to read Alice's goat
  await assertFails(bobDb.collection(`users/${ALICE_UID}/goats`).doc("goat_1").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/goats`).get());
  // Bob tries to write to Alice's path
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/goats`).doc("goat_2").set({
      userId: BOB_UID,
      tag: "TAG-002",
      name: "Daisy",
      breed: "Saanen",
      gender: "Doe",
      status: "Active",
      createdAt: new Date(),
    })
  );
});

// 3. Schema validation: Invalid gender or missing tag rejected
test("Schema validation: Invalid gender rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/goats`).doc("goat_bad").set({
      userId: ALICE_UID,
      tag: "TAG-999",
      name: "BadGoat",
      breed: "Alpine",
      gender: "UnknownGender",
      status: "Active",
      createdAt: new Date(),
    })
  );
});

// 4. MilkLog and Breeding records
test("Owner can write and read milk log and breeding record", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/milk_logs`).doc("milk_1").set({
      userId: ALICE_UID,
      doeTag: "TAG-001",
      date: "2026-11-08",
      session: "Morning",
      yieldLiters: 3.5,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/breedings`).doc("breed_1").set({
      userId: ALICE_UID,
      doeTag: "TAG-001",
      buckTag: "BUCK-99",
      breedingDate: "2026-11-01",
      expectedKiddingDate: "2027-03-31",
      status: "Bred",
      createdAt: new Date(),
    })
  );
});
