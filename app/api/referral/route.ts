import { NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';
import { createServerSupabaseClient } from '@/lib/supabase/server';

function getAdminClient() {
    return createClient(
        process.env.NEXT_PUBLIC_SUPABASE_URL!,
        process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
    );
}

export async function GET() {
    const supabase = await createServerSupabaseClient();
    const { data: { user } } = await supabase.auth.getUser();
    if (!user?.email) return new NextResponse('Unauthorized', { status: 401 });

    const supabaseAdmin = getAdminClient();

    const { data: profile, error } = await supabaseAdmin
        .from('profiles')
        .select('id, referral_code, referral_balance, referred_by')
        .eq('email', user.email)
        .single();

    if (error) return new NextResponse(error.message, { status: 500 });

    const { count } = await supabaseAdmin
        .from('referrals')
        .select('*', { count: 'exact', head: true })
        .eq('referrer_id', profile.id);

    return NextResponse.json({
        code: profile.referral_code,
        balance: profile.referral_balance,
        count: count || 0,
        referred_by: profile.referred_by,
    });
}

export async function POST(req: Request) {
    const supabase = await createServerSupabaseClient();
    const { data: { user } } = await supabase.auth.getUser();
    if (!user?.id) return new NextResponse('Unauthorized', { status: 401 });

    const { code } = await req.json();
    const supabaseAdmin = getAdminClient();

    const { data: referrer, error: refError } = await supabaseAdmin
        .from('profiles')
        .select('id')
        .eq('referral_code', code)
        .single();

    if (refError || !referrer) return new NextResponse('Código inválido', { status: 400 });
    if (referrer.id === user.id) return new NextResponse('No puedes referirte a ti mismo', { status: 400 });

    const { error: updateError } = await supabaseAdmin
        .from('profiles')
        .update({ referred_by: referrer.id })
        .eq('id', user.id);

    if (updateError) return new NextResponse(updateError.message, { status: 500 });

    await supabaseAdmin.from('referrals').insert({
        referrer_id: referrer.id,
        referred_id: user.id,
    });

    return NextResponse.json({ success: true });
}
