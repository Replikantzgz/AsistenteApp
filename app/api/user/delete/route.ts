import { NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth-options';
import { createClient } from '@supabase/supabase-js';
import Stripe from 'stripe';

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY!, {
    apiVersion: '2025-11-17.clover' as any, // Match existing codebase version
});

const supabaseAdmin = createClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!,
    {
        auth: {
            autoRefreshToken: false,
            persistSession: false
        }
    }
);

export async function DELETE(req: Request) {
    try {
        const session: any = await getServerSession(authOptions);

        // Check if user is authenticated
        if (!session?.user?.id) {
            return new NextResponse('Unauthorized', { status: 401 });
        }

        const userId = session.user.id;
        console.log(`[DELETE_ACCOUNT] Starting deletion for user: ${userId} (${session.user.email})`);

        // 1. Get user profile to find Stripe Customer ID
        const { data: profile, error: profileError } = await supabaseAdmin
            .from('profiles')
            .select('stripe_customer_id')
            .eq('id', userId)
            .single();

        if (profileError && profileError.code !== 'PGRST116') { // PGRST116 is "Row not found"
            console.error('[DELETE_ACCOUNT] Error fetching profile:', profileError);
            return new NextResponse('Error fetching user profile', { status: 500 });
        }

        // 2. Delete Stripe Customer if exists
        if (profile?.stripe_customer_id) {
            try {
                await stripe.customers.del(profile.stripe_customer_id);
                console.log(`[DELETE_ACCOUNT] Stripe customer deleted: ${profile.stripe_customer_id}`);
            } catch (stripeError: any) {
                console.error('[DELETE_ACCOUNT] Error deleting Stripe customer:', stripeError);
                // We continue even if Stripe fails, to ensure account is deleted from our system
                // potentially we could return 500 here if strict consistency is required,
                // but usually better to let the user leave.
            }
        }

        // 3. Delete User from Supabase Auth (Cascades to public.profiles, public.notes, etc.)
        const { error: deleteError } = await supabaseAdmin.auth.admin.deleteUser(userId);

        if (deleteError) {
            console.error('[DELETE_ACCOUNT] Error deleting Supabase user:', deleteError);
            return new NextResponse(`Error deleting account: ${deleteError.message}`, { status: 500 });
        }

        console.log(`[DELETE_ACCOUNT] Successfully deleted user ${userId}`);
        return NextResponse.json({ success: true });

    } catch (error: any) {
        console.error('[DELETE_ACCOUNT] Unexpected error:', error);
        return new NextResponse('Internal Server Error', { status: 500 });
    }
}
