package com.iatualcance.alfred

import com.iatualcance.alfred.ui.Look
import com.iatualcance.alfred.ui.Skins
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinTest {
    @Test fun idsAreUniqueAndFallbackIsPokemon() {
        assertEquals(Skins.all.size, Skins.all.map { it.id }.toSet().size)
        assertEquals(Skins.POKEMON, Skins.byId("no-existe"))
        assertEquals(Skins.MEDIANOCHE, Skins.byId("alfred_medianoche"))
    }
    @Test fun onlyPokemonUsesIllustrations() {
        assertTrue(Skins.POKEMON.pokemon)
        assertTrue(Skins.all.filter { it.id != "pokemon" }.none { it.pokemon })
        assertTrue(Skins.MEDIANOCHE.dark && !Skins.MARFIL.dark)
    }
    @Test fun themedSkinsAreRegistered() {
        assertEquals(8, Skins.all.size)
        assertEquals(Look.MARVEL, Skins.byId("marvel").look)
        assertEquals(Look.DC, Skins.byId("dc").look)
        assertEquals(Look.RETRO, Skins.byId("retro80").look)
        assertEquals(Look.CYBER, Skins.byId("cyberpunk").look)
        assertTrue(Skins.all.filter { it.look != Look.POKEMON }.none { it.pokemon })
    }
}
