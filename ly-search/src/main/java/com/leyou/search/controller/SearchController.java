package com.leyou.search.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.search.domain.SearchRequest;
import com.leyou.search.service.SearchService;
import item.dto.GoodsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 查询商城关键词搜索
     */
    @PostMapping("/page")
    public ResponseEntity<PageResult<GoodsDTO>> goodsPageQuery(@RequestBody SearchRequest searchRequest){
       PageResult<GoodsDTO> pageResult = searchService.goodsPageQuery(searchRequest);
       return ResponseEntity.ok(pageResult);
    }

    /**
     * 根据查询条件查询分类和品牌
     * @param searchRequest
     * @return
     */
    @PostMapping("/filter")
    public  ResponseEntity<Map<String , List<?>>> filterParamQuery(@RequestBody SearchRequest searchRequest){
        Map<String , List<?>> filterParamMap = searchService.filterParamQuery(searchRequest);
        return ResponseEntity.ok(filterParamMap);
    }

}
