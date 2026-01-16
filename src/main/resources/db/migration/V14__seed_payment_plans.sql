INSERT INTO price_plan (
    id,
    code,
    stripe_price_id,
    display_name,
    monthly_amount,
    annual_amount,
    max_topics_per_period,
    max_tokens_per_period,
    warning_threshold_ratio
) VALUES
-- Free plan (Discord / default)
(
    gen_random_uuid(),
    'FREE',
    'FREE_INTERNAL',               -- not a real Stripe price
    'Free',
    0,
    0,
    20,
    50000,
    0.80
),

-- Pro Monthly
(
    gen_random_uuid(),
    'PRO_MONTHLY',
    'price_1Sq2rpDhdDJ6p07kUw9mOEsb',       -- replace with real Stripe price ID
    'Pro (Monthly)',
    1000,                          -- $10.00
    NULL,
    200,
    1000000,
    0.80
),

-- Pro Annual
(
    gen_random_uuid(),
    'PRO_ANNUAL',
    'price_1Sq2sbDhdDJ6p07kvbwVvd79',        -- replace with real Stripe price ID
    'Pro (Annual)',
    NULL,
    10000,                         -- $100.00 ($8.33/mo)
    200,
    1000000,
    0.80
)
;
