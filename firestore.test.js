const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid";
const BOB_UID = "bob_uid";

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

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read users or chats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("chats").get());
});

test("Authenticated user: Alice can create her profile with valid username", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_wonder",
      displayName: "Alice",
      statusMessage: "Hey there! I am using AliasChat.",
      isOnline: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot impersonate another user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_builder",
      displayName: "Bob",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Unique Username: Alice can reserve her username", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("usernames").doc("alice_wonder").set({
      username: "alice_wonder",
      userId: ALICE_UID,
      createdAt: new Date(),
    })
  );
});

test("Unique Username: Bob cannot overwrite Alice's username", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("usernames").doc("alice_wonder").set({
      username: "alice_wonder",
      userId: ALICE_UID,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("usernames").doc("alice_wonder").set({
      username: "alice_wonder",
      userId: BOB_UID,
      createdAt: new Date(),
    })
  );
});

test("Chats: Alice can create a chat including herself and Bob", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_123").set({
      chatId: "chat_123",
      type: "direct",
      memberUids: [ALICE_UID, BOB_UID],
      memberUsernames: ["alice", "bob"],
      lastMessage: "Hi Bob!",
      lastMessageSenderId: ALICE_UID,
      lastMessageSenderUsername: "alice",
      lastMessageTime: new Date(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Chats: Charlie cannot read Alice and Bob's chat", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_123").set({
      chatId: "chat_123",
      type: "direct",
      memberUids: [ALICE_UID, BOB_UID],
      memberUsernames: ["alice", "bob"],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const charlieDb = testEnv.authenticatedContext("charlie_uid").firestore();
  await assertFails(charlieDb.collection("chats").doc("chat_123").get());
});

test("Messages: Alice can send message in her chat", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_123").set({
      chatId: "chat_123",
      type: "direct",
      memberUids: [ALICE_UID, BOB_UID],
      memberUsernames: ["alice", "bob"],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_123").collection("messages").doc("msg_1").set({
      id: "msg_1",
      chatId: "chat_123",
      senderId: ALICE_UID,
      senderUsername: "alice",
      text: "Hello!",
      type: "text",
      createdAt: new Date(),
    })
  );
});

test("Calls: Alice can initiate a call session to Bob", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_999").set({
      callId: "call_999",
      callerId: ALICE_UID,
      callerUsername: "alice",
      callerName: "Alice W",
      receiverId: BOB_UID,
      receiverUsername: "bob",
      memberUids: [ALICE_UID, BOB_UID],
      callType: "audio",
      status: "ringing",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
