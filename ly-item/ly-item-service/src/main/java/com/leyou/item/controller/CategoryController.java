package com.leyou.item.controller;


import com.leyou.item.service.CategoryService;
import item.dto.CategoryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 请求的地址：/item/category/of/parent
     * 请求的参数：GET /category/of/parent?pid=0
     * 返回的数据： List集合
     */
    @GetMapping("/of/parent")
    public ResponseEntity<List<CategoryDTO>> queryByParentId(@RequestParam("pid") Long pid){

        return ResponseEntity.ok(categoryService.queryListByParent(pid));
    }

    /**
     * 根据id查询分类信息
     * @param ids
     * @return
     */
    @GetMapping("/list")
    public ResponseEntity<List<CategoryDTO>> findCategoryByIds(@RequestParam("ids") List ids){
        List<CategoryDTO> list = categoryService.findCategoryByIds(ids);

        return ResponseEntity.ok(list);
    }
}
