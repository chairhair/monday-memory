MondayMemory — Memory Architecture Spec v1.0

Last updated: 2025-11
Owner: Joe Brock
Applies to: Extension, Backend, Discord Bot, LLM clients
Scope: SessionMemory, TopicMemory, Memory Querying, Rollup, Exemplars, Quotas, and Delete Policies

🧭 A. Identity & Ownership Rules
1. Topics belong to users, always.

Each TopicMemory has exactly one userId owner.

Topics are long-term, user-owned knowledge containers.

Guests never own topics.

2. Sessions belong to a user and may involve guests.

Each SessionMemory has a userId owner.

A session may reference one or more guestIds.

A session may have a topicId (if linked) or none.

3. Guests do not get topics.

Guests have only ephemeral sessions.

Guests cannot recall history, cannot edit topics, and do not have long-term memory.

Guests may appear as metadata in a user’s topics.

4. One meaning per concept.

Topic = Persistent, structured knowledge owned by a user.

Session = Short-lived interaction log.

Guest = participant.

No overloaded semantics.

⏳ B. Lifetime, Aging & Retention Rules
5. Sessions are short-lived.

Raw session bodies only guaranteed to exist for a fresh window (e.g., 0–7 days).

Within this window: raw text, detailed highlights, embeddings remain.

6. After the fresh window, sessions are distilled.

Produce a SessionSnapshot (1–3 sentence summary).

Attach snapshot to the topic’s timeline (if topic exists).

Raw bodies and heavy embeddings deleted unless session is an exemplar.

7. Topics are long-lived.

Topics live indefinitely until:

user archives,

or user deletes,

or (rare) inactivity retention policy triggers deletion.

8. Linking a session to a topic does NOT extend raw retention.

Only the meaning is preserved, not the raw text.

Topic retains:

summary,

timeline checkpoints,

exemplars.

9. No memory exists in limbo.

Every session must be in exactly one state:

FRESH

COMPRESSED (snapshot only or exemplar)

DELETED

🔍 C. Query & Recall Rules
10. Sessions never fetch other sessions.

No entity-level logic where one session queries others.

Prevents graph explosion & N+1 hell.

11. All cross-memory logic goes through MemoryQueryService.

Allowed examples:

getContextForSession(sessionId)

getTopicContext(topicId)

searchMemory(text, userId)

12. Recall follows strict precedence:

Topic context (if topic exists)

Fresh sessions

Broader search of topics + snapshots

13. Context size is hard-limited.

Max highlights returned

Max exemplars used

Max timeline entries

Max tokens

14. Older memory = more compressed.

Recency → highest fidelity

1–3 months → medium

6–12 months → summary-only

12 months → summary + timeline

🎬 D. Exemplar Session Rules (5 per Topic)
15. Each topic keeps up to 5 exemplar sessions.

These represent the “key chapters” of the topic.

16. Exemplars are explicit.

Stored in TopicExemplar table/entity with fields:

topicId

sessionId

representativenessScore

role (CORE, TURNING_POINT, RECENT, etc.)

lastRefreshedAt

17. Representativeness scoring formula.

Score computed using:

cosine similarity to topic embedding

recency

weight (frequency, importance)

optional user signals (starred/pinned)

18. Exemplar selection during roll-up:

For each topic:

Identify aged-out sessions.

Compute representativeness.

Choose:

2–3 core exemplars

1–2 turning point / high-weight sessions

1 recent exemplar

Cap at 5, drop lowest score.

19. Non-exemplar aged sessions trimmed aggressively.

Keep only:

snapshot

minimal metadata

Delete raw body + heavy embeddings.

20. Exemplars obey retention but are privileged.

They survive when normal sessions are trimmed.

User delete or topic deletion removes them.

📘 E. Topic Storyline & Checkpoint Rules
21. Topics have a structured timeline.

Each topic maintains:

chronologically ordered HistoryCheckpoint objects:

timestamp

snapshot text

optional link to exemplar

type (decision, milestone, todo, issue)

22. Topics track a “Where you left off” field.

lastCheckpoint

nextActions
This ensures user returns to a coherent continuation.

23. Pruning must preserve story.

Raw deletes must not remove:

timeline entries

checkpoints

summaries

exemplars

All raw→snapshot roll-up must occur before deletion.

🧼 F. Deletion, Archiving & Privacy Rules
24. Delete means DELETE.

A user-initiated delete removes:

topic

snapshots

exemplars

embeddings

cached items

event queue entries

25. Archive (safe delete) vs Full delete.

Archive / Safe Delete

archived = true

Hidden from normal recall

Recoverable

Stored indefinitely until user touches it or retention triggers

Full Delete

Permanently removed

Triggered by:

explicit user request

retention rule: archived > 6 months with zero access

26. Topics persist until user says otherwise.

System never auto-deletes active topics.

27. Guests cannot own deletable topics.

Guest delete requests map only to user-owned data that references the guest.

⚙️ G. Performance & Implementation Discipline
28. Fresh recall must feel instant.

< 400ms with cache

~1s with DB + summary assembly

Slow responses must degrade gracefully

Use Redis where helpful

29. No recall should depend solely on re-summarizing.

Summaries:

are persisted

are updated only when enough new memory arrives

can be explicitly refreshed by user

30. Memory services must be centralized and testable.

Services include:

MemoryQueryService

MemoryWriterService

MemoryRollupService

They must have clear contracts and no hidden DB gymnastics.

31. No invisible retention.

If long-term info is stored:

it must be intentional,

documented,

and visible in UI/API.

💳 H. Topic Quota & Billing Rules
32. Free-tier users have a strict cap of 25 topics.

This includes:

active topics

archived topics

topics with exemplars

empty topics
If topicCount >= 25, new topic creation is blocked with a clear upgrade prompt.

33. Topic creation fails cleanly at quota.

Return:

402 PAYMENT_REQUIRED
or

409 CONFLICT with helpful payload:

“You've reached the free limit of 25 topics. Upgrade to create more.”

34. Sessions still work when at cap.

Users can continue to record sessions

But no new topics created when cap reached

Those sessions stay topic-less until user:

deletes a topic

or upgrades

35. Topic splitting inhibited when at cap.

If a topic is so large it should be split:

Split is skipped

Topic is hard-compressed instead

User notified:

“This topic should be split, but you’re at your topic limit.”

36. Archived topics still count toward the 25-topic cap.

Only full delete frees slots.

37. Pro / Enterprise tiers remove or raise the cap.

Clear upgrade path:

Free → 25 topics

Pro → e.g. 200 topics or unlimited

Enterprise → unlimited + shared topics later

38. Exemplars do NOT count toward the cap.

Only topics count.

🔒 I. Security, Compliance, and Privacy Notes
39. Strict boundary between guest data and user-owned topics.

Guest presence is metadata only.

40. No auto-recording without explicit user signal.

Discord and extension must clearly indicate when recording is active.

41. User-visible audit trail for memory.

User can always:

view topics

view session snapshots

delete/rename/clean

42. GDPR/CCPA-safe retention.

Full delete honors “right to be forgotten”

Timeline and exemplars are included in delete operations

No shadow copies