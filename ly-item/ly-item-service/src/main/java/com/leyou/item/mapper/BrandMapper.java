package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import com.leyou.item.domain.Brand;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BrandMapper extends BaseMapper<Brand> {
    void saveCategoryId(@Param("id") Long id,@Param("cids") List<Long> cids);

    @Select("select * from tb_brand b,tb_category_brand cb where b.id = cb.brand_id and cb.category_id = #{id}")
    List<Brand> findBrandByCategoryId(Long id);
}
