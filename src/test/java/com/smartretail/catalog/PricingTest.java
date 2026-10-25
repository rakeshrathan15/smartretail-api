package com.smartretail.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartretail.catalog.CatalogDtos.SlabRequest;
import com.smartretail.common.ApiException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingTest {

    private static Product colaWithSlabs() {
        Product product = new Product("Cola", "cola", new BigDecimal("420"));
        product.addSlab(25, null, new BigDecimal("357"));
        product.addSlab(10, 24, new BigDecimal("378"));
        return product;
    }

    @Test
    void appliesTheHighestSlabReached() {
        Product cola = colaWithSlabs();
        assertThat(cola.pricePerCaseFor(1)).isEqualByComparingTo("420");
        assertThat(cola.pricePerCaseFor(9)).isEqualByComparingTo("420");
        assertThat(cola.pricePerCaseFor(10)).isEqualByComparingTo("378");
        assertThat(cola.pricePerCaseFor(24)).isEqualByComparingTo("378");
        assertThat(cola.pricePerCaseFor(25)).isEqualByComparingTo("357");
        assertThat(cola.pricePerCaseFor(500)).isEqualByComparingTo("357");
        assertThat(cola.lowestPricePerCase()).isEqualByComparingTo("357");
    }

    @Test
    void usesBasePriceWithoutSlabs() {
        Product water = new Product("Water", "water", new BigDecimal("240"));
        assertThat(water.pricePerCaseFor(100)).isEqualByComparingTo("240");
        assertThat(water.lowestPricePerCase()).isEqualByComparingTo("240");
    }

    @Test
    void validSlabsComeBackSorted() {
        List<SlabRequest> sorted = AdminCatalogService.validateSlabs(List.of(
                new SlabRequest(25, null, new BigDecimal("357")),
                new SlabRequest(10, 24, new BigDecimal("378"))), new BigDecimal("420"));
        assertThat(sorted).extracting(SlabRequest::minQty).containsExactly(10, 25);
    }

    @Test
    void rejectsBadSlabs() {
        BigDecimal base = new BigDecimal("420");
        assertThatThrownBy(() -> AdminCatalogService.validateSlabs(List.of(
                new SlabRequest(10, null, new BigDecimal("400")),
                new SlabRequest(10, null, new BigDecimal("390"))), base))
                .isInstanceOf(ApiException.class).hasMessageContaining("Two slabs start at 10");
        assertThatThrownBy(() -> AdminCatalogService.validateSlabs(List.of(
                new SlabRequest(10, 30, new BigDecimal("400")),
                new SlabRequest(25, null, new BigDecimal("390"))), base))
                .isInstanceOf(ApiException.class).hasMessageContaining("overlap");
        assertThatThrownBy(() -> AdminCatalogService.validateSlabs(List.of(
                new SlabRequest(10, 5, new BigDecimal("400"))), base))
                .isInstanceOf(ApiException.class).hasMessageContaining("Max cases");
        assertThatThrownBy(() -> AdminCatalogService.validateSlabs(List.of(
                new SlabRequest(10, null, new BigDecimal("450"))), base))
                .isInstanceOf(ApiException.class).hasMessageContaining("above the base price");
    }

    @Test
    void slugsAreUrlSafeAndUnique() {
        assertThat(Slugs.of("Sparkling / Lime")).isEqualTo("sparkling-lime");
        assertThat(Slugs.of("  Thums Up 750ml PET ")).isEqualTo("thums-up-750ml-pet");
        assertThat(Slugs.unique("Cola", s -> s.equals("cola") || s.equals("cola-2"))).isEqualTo("cola-3");
    }
}
