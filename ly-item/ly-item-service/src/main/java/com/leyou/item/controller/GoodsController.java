package com.leyou.item.controller;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.service.GoodsService;
import item.dto.SkuDTO;
import item.dto.SpuDTO;
import item.dto.SpuDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    /**
     * 显示spu列表
     * @param page
     * @param rows
     * @param key
     * @param saleable
     * @return
     */
    @GetMapping("/spu/page")
    public ResponseEntity<PageResult<SpuDTO>> findSpuPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "saleable", required = false) Boolean saleable
    ) {
        PageResult<SpuDTO> list = goodsService.findSpuPage(page, rows, key, saleable);

        return ResponseEntity.ok(list);
    }

    /**
     * 保存商品
     * @param spuDTO
     * @return
     */
    @PostMapping("/goods")
    public ResponseEntity<Void> saveGoods(@RequestBody SpuDTO spuDTO) {
        goodsService.saveGoods(spuDTO);

        return ResponseEntity.status(HttpStatus.CREATED).build();//201
    }

    /**
     * 商品上下架
     * @param id
     * @param saleable
     * @return
     */
    @PutMapping("/spu/saleable")
    public ResponseEntity<Void> updateSaleable(@RequestParam("id")Long id,
                                               @RequestParam("saleable")Boolean saleable){
        goodsService.updateSaleable(id,saleable);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();//204
    }

    /**
     * 根据spu id 查询详细信息
     * @param id
     * @return
     */
    @GetMapping("/spu/detail")
    public ResponseEntity<SpuDetailDTO> findSpuDetail(@RequestParam("id")Long id){
        SpuDetailDTO spuDetailDTO = goodsService.findSpuDetail(id);
        return ResponseEntity.ok(spuDetailDTO);
    }

    /**
     * 根据spuid查询sku
     * @param id
     * @return
     */
    @GetMapping("/sku/of/spu")
    public ResponseEntity<List<SkuDTO>> findSkuBySpuId(@RequestParam("id")Long id){
        List<SkuDTO> skuDTOS = goodsService.findSkuBySpuId(id);
        return ResponseEntity.ok(skuDTOS);
    }

    /**
     * 新增商品信息
     * @param spuDTO
     * @return
     */
    @PutMapping("/goods")
    public ResponseEntity<Void> updateGoods(@RequestBody SpuDTO spuDTO){
        goodsService.updateGoods(spuDTO);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 根据spuId查询spu信息
     */
    @GetMapping("/spu/{id}")
    public ResponseEntity<SpuDTO> findSpuById(@PathVariable("id") Long id){
        //根据id 查询spuDto
        SpuDTO spuDTO = goodsService.findSpuById(id);
        return ResponseEntity.ok(spuDTO);
    }

}
