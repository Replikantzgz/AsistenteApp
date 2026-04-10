import { NextResponse } from 'next/server';
import Stripe from 'stripe';
import { createServerSupabaseClient } from '@/lib/supabase/server';

function getStripe() {
    return new Stripe(process.env.STRIPE_SECRET_KEY!, {
        apiVersion: '2025-11-17.clover' as any,
    });
}

export async function POST(req: Request) {
    try {
        const supabase = await createServerSupabaseClient();
        const { data: { user } } = await supabase.auth.getUser();

        if (!user?.email) {
            return new NextResponse('Unauthorized: Please sign in first', { status: 401 });
        }

        const { plan } = await req.json();

        const priceId = plan === 'pro'
            ? process.env.STRIPE_PRICE_ID_PRO
            : process.env.STRIPE_PRICE_ID_ECO;

        if (!priceId || priceId === 'price_replace_me') {
            const missingVar = plan === 'pro' ? 'STRIPE_PRICE_ID_PRO' : 'STRIPE_PRICE_ID_ECO';
            return new NextResponse(`Stripe Price ID (${missingVar}) not configured`, { status: 500 });
        }

        const stripe = getStripe();
        const checkoutSession = await stripe.checkout.sessions.create({
            mode: 'subscription',
            payment_method_types: ['card'],
            line_items: [{ price: priceId, quantity: 1 }],
            subscription_data: { trial_period_days: 7 },
            success_url: `${process.env.NEXT_PUBLIC_APP_URL}/?success=true`,
            cancel_url: `${process.env.NEXT_PUBLIC_APP_URL}/?canceled=true`,
            customer_email: user.email,
            metadata: {
                userId: user.id,
                userEmail: user.email,
                plan,
            },
        });

        return NextResponse.json({ url: checkoutSession.url });
    } catch (error: any) {
        console.error('[STRIPE_CHECKOUT]', error);
        return new NextResponse(error.message, { status: 500 });
    }
}
