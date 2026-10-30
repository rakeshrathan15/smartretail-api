package com.smartretail.picklist;

import com.smartretail.common.RouteFilter;
import com.smartretail.picklist.PickListService.PickList;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The van loading list for a route and delivery date. Admin-only (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/pick-list")
public class AdminPickListController {

    private final PickListService pickLists;

    public AdminPickListController(PickListService pickLists) {
        this.pickLists = pickLists;
    }

    /** {@code route} is a route id, or {@code unassigned} for orders from shops on no route. */
    @GetMapping
    public PickList get(@RequestParam String route,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return pickLists.forRoute(RouteFilter.parse(route), date);
    }
}
