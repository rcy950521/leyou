package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.domain.Category;
import com.leyou.item.mapper.CategoryMapper;
import item.dto.CategoryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    //导入mapper
    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据父id查询
     */
    public List<CategoryDTO> queryListByParent(Long pid){
        //创建一个category对象
        Category category = new Category();
        category.setParentId(pid);

        List<Category> categoryList = categoryMapper.select(category);

        if (CollectionUtils.isEmpty(categoryList)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }

        //将categoryList转成dto对象
        List<CategoryDTO> list = BeanHelper.copyWithCollection(categoryList, CategoryDTO.class);

        return list;
    }

    /**
     *
     * @param ids
     * @return
     */
    public List<CategoryDTO> findCategoryByIds(List ids) {
        List list  = categoryMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list,CategoryDTO.class);
    }
}
