package com.example.shop.common.code;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 공통코드 매퍼. 사용 중(use_yn = 'Y')인 코드만 돌려준다. SQL은 {@code resources/mapper/common/CommonCodeMapper.xml}.
 */
@Mapper
public interface CommonCodeMapper {

    /** 그룹의 코드 목록을 정렬 순서대로 조회한다. */
    List<CommonCode> selectCodeList(@Param("groupCode") String groupCode);

    /** 코드 한 건을 조회한다. */
    Optional<CommonCode> selectCodeOne(@Param("groupCode") String groupCode, @Param("code") String code);
}
