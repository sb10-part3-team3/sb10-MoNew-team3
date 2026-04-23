package com.team3.monew.repository;

import com.team3.monew.dto.interest.internal.InterestSearchCondition;
import com.team3.monew.entity.Interest;
import java.util.List;

public interface InterestRepositoryCustom {

  List<Interest> searchByCondition(InterestSearchCondition condition);

  long countByCondition(InterestSearchCondition condition);

}
