import { headers } from 'next/headers';
import { NextResponse } from 'next/server';
import Stripe from 'stripe';
import { createClient } from '@supabase/supabase-js';

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY!, {
    apiVersion: '2025-01-27.acacia',
});

// Admin client for webhook updates
const supabase = createClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!
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
        return new NextResponse(`Webhook Error: ${error.message}`, { status: 400 });
    }

    const session = event.data.object as Stripe.Checkout.Session;

    if (event.type === 'checkout.session.completed') {
        const subscription = await stripe.subscriptions.retrieve(session.subscription as string);

        if (!session?.metadata?.userId) {
            return new NextResponse('User ID missing in metadata', { status: 400 });
        }

        // Update user profile in Supabase
        // Note: We are using email as userId in metadata for mapping, 
        // but in Supabase we should ideally look up by email if we don't have the UUID.
        // Assuming profiles are synced by email.

        const { error } = await supabase
            .from('profiles')
            .update({
                stripe_customer_id: subscription.customer as string,
                subscription_status: 'active',
                plan: session.metadata.plan,
                updated_at: new Date().toISOString(),
            })
            .eq('email', session.metadata.userId); // Using email to match

        if (error) {
            console.error('Supabase Update Error:', error);
            return new NextResponse('Database Update Failed', { status: 500 });
        }
    }

    if (event.type === 'customer.subscription.updated') {
        // Handle renewals/cancellations
        // ...
    }

    return new NextResponse(null, { status: 200 });
}
