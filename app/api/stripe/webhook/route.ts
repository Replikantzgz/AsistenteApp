import { headers } from 'next/headers';
import { NextResponse } from 'next/server';
import Stripe from 'stripe';
import { createClient } from '@supabase/supabase-js';

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY!, {
    apiVersion: '2025-11-17.clover' as any,
});

// Admin client for webhook updates using Service Role (simulated if missing)
// NOTE: Make sure SUPABASE_SERVICE_ROLE_KEY is set in production
const supabase = createClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY! // Fallback to Anon (will fail RLS without policy, user should add Service Role)
);

export async function POST(req: Request) {
    const body = await req.text();
    const signature = (await headers()).get('Stripe-Signature') as string;

    let event: Stripe.Event;

    try {
        event = stripe.webhooks.constructEvent(
            body,
            signature,
            process.env.STRIPE_WEBHOOK_SECRET!
        );
    } catch (error: any) {
        console.error('Webhook verification failed:', error.message);
        return new NextResponse(`Webhook Error: ${error.message}`, { status: 400 });
    }

    const session = event.data.object as Stripe.Checkout.Session;

    if (event.type === 'checkout.session.completed') {
        const subscription = await stripe.subscriptions.retrieve(session.subscription as string);

        if (!session?.metadata?.userId) {
            return new NextResponse('User ID missing in metadata', { status: 400 });
        }

        // Update user profile in Supabase
        const { error } = await supabase
            .from('profiles')
            .update({
                stripe_customer_id: subscription.customer as string,
                subscription_status: 'active',
                plan: session.metadata.plan || 'pro',
                updated_at: new Date().toISOString(),
            })
            .eq('id', session.metadata.userId);

        if (error) {
            console.error('Supabase Update Error:', error);
            // Don't fail the webhook response to avoid retries if it's a logic error
        }
    }

    if (event.type === 'customer.subscription.deleted') {
        const subscription = event.data.object as Stripe.Subscription;
        // In a real app, find user by customer_id and downgrade them
        console.log('Subscription deleted:', subscription.id);
    }

    return new NextResponse(null, { status: 200 });
}
