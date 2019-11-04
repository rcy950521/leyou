package com.leyou.item.client;

import com.leyou.common.pojo.PageResult;
import item.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("item-service")
public interface ItemClient {

    /**
     * 根据分类id和规格组id查询规格参数
     * @param gid
     * @param cid
     * @param searching
     * @return
     */
    @GetMapping("/spec/params")
    public List<SpecParamDTO> findSpecParam(
            @RequestParam(value = "gid",required = false)Long gid,
            @RequestParam(value = "cid",required = false)Long cid,
            @RequestParam(value = "searching",required = false)Boolean searching
    );

    /**
     * 根据spuid查询spudetail
     * @param id
     * @return
     */
    @GetMapping("/spu/detail")
    public SpuDetailDTO findSpuDetail(@RequestParam("id")Long id);

    /**
     * 根据spuid查询sku
     * @param id
     * @return
     */
    @GetMapping("/sku/of/spu")
    public List<SkuDTO> findSkuBySpuId(@RequestParam("id")Long id);

    /**
     * 显示spu列表
     * @param page
     * @param rows
     * @param key
     * @param saleable
     * @return
     */
    @GetMapping("/spu/page")
    public PageResult<SpuDTO> findSpuPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "saleable", required = false) Boolean saleable
    );

    /**
     * 根据品牌的id获取品牌信息
     * @param id
     * @return
     */
    @GetMapping("/brand/{id}")
    public BrandDTO findBrandById(@PathVariable("id") Long id);

    /**
     * 根据分类id查询分类信息
     * @param ids
     * @return
     */
    @GetMapping("/category/list")
    public List<CategoryDTO> findCategoryByIds(@RequestParam("ids") List<Long> ids);

    /**
     * 根据spuId查询spu信息
     */
    @GetMapping("/spu/{id}")
    public SpuDTO findSpuById(@PathVariable("id") Long id);

    //根据categoryId查询规格参数组和组内参数
    @GetMapping("/spec/of/category")
    public List<SpecGroupDTO> findSpecGroupAndParamByCid(@RequestParam("id") Long id);
}
