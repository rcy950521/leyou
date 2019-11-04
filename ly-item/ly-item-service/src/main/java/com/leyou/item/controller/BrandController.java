package com.leyou.item.controller;


import com.leyou.common.pojo.PageResult;
import com.leyou.item.service.BrandService;
import item.dto.BrandDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    /**
     * 访问的地址 ： /brand/page?key=&page=1&rows=5&sortBy=id&desc=false
     * 请求的参数：key=&page=1&rows=5&sortBy=id&desc=false
     * 返回的数据：封装后的分页信息
     * @return
     */
    @GetMapping("/page")
    public ResponseEntity<PageResult<BrandDTO>> brandQueryPage(
        @RequestParam(value = "key",required = false) String key,
        @RequestParam(value = "page",defaultValue = "1") Integer page,
        @RequestParam(value = "rows",defaultValue = "5") Integer rows,
        @RequestParam(value = "sortBy",defaultValue = "id") String sortBy,
        @RequestParam(value = "desc",defaultValue = "false") Boolean desc
    ){
        PageResult<BrandDTO> result = brandService.brandQueryPage(key,page,rows,sortBy,desc);
        return ResponseEntity.ok(result);
    }

    /**
     * 请求地址：POST /brand
     * 请求的参数：
     * 返回的数据：无
     */
    @PostMapping
    public  ResponseEntity<Void> saveBrand(BrandDTO brandDTO, @RequestParam("cids")List<Long> cids){

        brandService.saveBrandAndCategory(brandDTO,cids);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据品牌的id获取品牌信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<BrandDTO> findBrandById(@PathVariable("id") Long id){
      BrandDTO brandDTO = brandService.findBrandById(id);
      return ResponseEntity.ok(brandDTO);
    }

    @GetMapping("/of/category")
    public ResponseEntity<List<BrandDTO>> findBrandByCategoryId(@RequestParam("id") Long id){

        List<BrandDTO> list = brandService.findBrandByCategoryId(id);

        return ResponseEntity.ok(list);
    }
}
