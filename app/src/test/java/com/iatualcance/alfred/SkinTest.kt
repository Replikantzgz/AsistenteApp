package com.iatualcance.alfred

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
}
