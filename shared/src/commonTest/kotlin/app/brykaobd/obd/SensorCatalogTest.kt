package app.brykaobd.obd

import kotlin.test.Test
import kotlin.test.assertTrue

class SensorCatalogTest {
    @Test
    fun catalog_includes_gauges_dpf_and_extras() {
        val requests = SensorCatalog.all.map { it.request }.toSet()
        assertTrue(requests.contains("010D"))
        assertTrue(requests.contains("221470"))
        assertTrue(requests.contains("223275"))
        assertTrue(requests.contains("0104"))
        assertTrue(SensorCatalog.all.size >= 20)
    }

    @Test
    fun search_olej_finds_oil_related() {
        val hits = SensorCatalog.search("olej")
        assertTrue(hits.any { it.request == "015C" })
        assertTrue(hits.any { it.request == "221470" })
        assertTrue(hits.size < 20)
    }

    @Test
    fun search_empty_returns_all() {
        assertTrue(SensorCatalog.search("").size == SensorCatalog.all.size)
    }
}
