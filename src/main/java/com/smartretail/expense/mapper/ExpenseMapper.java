package com.smartretail.expense.mapper;

import com.smartretail.common.mapper.MapStructConfig;
import com.smartretail.expense.dto.response.ExpenseResponse;
import com.smartretail.expense.entity.Expense;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface ExpenseMapper {

    ExpenseResponse toResponse(Expense e);
}
