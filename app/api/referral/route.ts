import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth-options';
import { NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
);

export async function GET() {
    const session: any = await getServerSession(authOptions);
    if (!session?.user?.email) return new NextResponse('Unauthorized', { status: 401 });

    const { data: profile, error } = await supabase
        .from('profiles')
        .select('id, referral_code, referral_balance, referred_by')
        .eq('email', session.user.email)
        .single();

    if (error) return new NextResponse(error.message, { status: 500 });

    // Get total referrals
    const { count } = await supabase
        .from('referrals')
        .select('*', { count: 'exact', head: true })
        .eq('referrer_id', profile.id);

    return NextResponse.json({
        code: profile.referral_code,
        balance: profile.referral_balance,
        count: count || 0,
        referred_by: profile.referred_by
    });
}

export async function POST(req: Request) {
    const session: any = await getServerSession(authOptions);
    if (!session?.user?.id) return new NextResponse('Unauthorized', { status: 401 });

    const { code } = await req.json();

    // 1. Find the referrer
    const { data: referrer, error: refError } = await supabase
        .from('profiles')
        .select('id')
        .eq('referral_code', code)
        .single();

    if (refError || !referrer) return new NextResponse('Código inválido', { status: 400 });
    if (referrer.id === session.user.id) return new NextResponse('No puedes referirte a ti mismo', { status: 400 });

    // 2. Update the referred user's profile
    const { error: updateError } = await supabase
        .from('profiles')
        .update({ referred_by: referrer.id })
        .eq('id', session.user.id);

    if (updateError) return new NextResponse(updateError.message, { status: 500 });

    // 3. Create entry in referrals table
    await supabase.from('referrals').insert({
        referrer_id: referrer.id,
        referred_id: session.user.id
    });

    return NextResponse.json({ success: true });
}
