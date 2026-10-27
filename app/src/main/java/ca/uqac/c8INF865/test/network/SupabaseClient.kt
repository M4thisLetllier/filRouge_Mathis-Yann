package ca.uqac.c8INF865.test.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object Supabase {
    // TODO: Tu remplaceras ces valeurs par celles de ton projet sur supabase.com j
    private const val SUPABASE_URL = "https://zwqsijdveobcmzfoltpg.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_bp1s9u-iPvvtI5h2eDNiJg_RtTOqfTY"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest) // Base de données
        install(Storage)   // Fichiers
        install(Auth)      // Authentification
    }
}
