package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.domain.Brand;
import com.leyou.item.mapper.BrandMapper;
import item.dto.BrandDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
@Transactional
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    /**
     * 查询品牌信息并分页
     *
     * @param key
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @return
     */
    public PageResult<BrandDTO> brandQueryPage(String key, Integer page, Integer rows, String sortBy, Boolean desc) {
        PageHelper.startPage(page, rows);

        Example example = new Example(Brand.class);

        if (StringUtils.isNotBlank(key)) {
            Example.Criteria criteria = example.createCriteria();
            criteria.orLike("name", "%" + key + "%");
            criteria.orEqualTo("letter", key.toUpperCase());
        }

        example.setOrderByClause(sortBy + " " + (desc ? "desc" : "asc"));
        List<Brand> brands = brandMapper.selectByExample(example);

        if (CollectionUtils.isEmpty(brands)) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        ;

        PageInfo<Brand> pageInfo = new PageInfo<>(brands);

        Long total = pageInfo.getTotal();

        List<BrandDTO> brandDTOS = BeanHelper.copyWithCollection(pageInfo.getList(), BrandDTO.class);

        PageResult<BrandDTO> result = new PageResult<>(total, brandDTOS);

        return result;
    }

    /**
     * 保存品牌信息
     *
     * @param brandDTO
     * @param cids
     */
    public void saveBrandAndCategory(BrandDTO brandDTO, List<Long> cids) {

        try {

            Brand brand = BeanHelper.copyProperties(brandDTO, Brand.class);

            brandMapper.insert(brand);

            brandMapper.saveCategoryId(brand.getId(), cids);
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }

    /**
     * 根据id查询品牌信息
     * @param id
     * @return
     */
    public BrandDTO findBrandById(Long id) {
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if (brand==null){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return BeanHelper.copyProperties(brand,BrandDTO.class);
    }

    /**
     * 根据分类id查询品牌信息
     * @param id
     * @return
     */
    public List<BrandDTO> findBrandByCategoryId(Long id) {

        List<Brand> list = brandMapper.findBrandByCategoryId(id);

        if (CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }

        return BeanHelper.copyWithCollection(list,BrandDTO.class);
    }
}

