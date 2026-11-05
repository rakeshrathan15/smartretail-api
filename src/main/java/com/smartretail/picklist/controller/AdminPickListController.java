package com.smartretail.picklist.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.common.util.RouteFilter;
import com.smartretail.picklist.dto.response.PickList;
import com.smartretail.picklist.service.PickListService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The van loading list for a route and delivery date. Admin-only (SecurityConfig). */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/pick-list")
public class AdminPickListController {

    private final PickListService pickLists;

    /** {@code route} is a route id, or {@code unassigned} for orders from shops on no route. */
    @GetMapping
    public ApiResponse<PickList> get(@RequestParam String route,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(pickLists.forRoute(RouteFilter.parse(route), date));
    }
}
