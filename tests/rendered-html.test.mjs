import assert from "node:assert/strict";
import test from "node:test";

async function render() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request("http://localhost/", {
      headers: { accept: "text/html", host: "localhost" },
    }),
    {
      ASSETS: {
        fetch: async () => new Response("Not found", { status: 404 }),
      },
    },
    {
      waitUntil() {},
      passThroughOnException() {},
    },
  );
}

test("server-renders the Atom prototype", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<title>Atom — Personal Reminder Prototype<\/title>/i);
  assert.match(html, /(?:Hello|Good (?:morning|afternoon|evening|night))/);
  assert.match(html, /Dhiren Sir/);
  assert.match(html, /Tell Atom what to remember/);
  assert.match(html, /Send product brief to Aisha/);
  assert.match(html, /Recurring reminder/);
  assert.match(html, /Switch to dark mode/);
  assert.match(html, /Atom waves hello/);
  assert.match(html, /ATOM MARK STUDIES/);
  assert.match(html, /Preview Orbit Atom logo/);
  assert.match(html, /Open Atom logo ideas/);
  assert.match(html, /Preview Nucleus Atom logo/);
  assert.match(html, /Preview Arc Atom logo/);
  assert.match(html, /Phase 1/);
  assert.doesNotMatch(html, /codex-preview/);
  assert.doesNotMatch(html, /Your site is taking shape/);
});
