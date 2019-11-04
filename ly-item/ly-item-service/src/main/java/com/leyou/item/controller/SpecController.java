package com.leyou.item.controller;

import com.leyou.item.service.SpecService;
import item.dto.SpecGroupDTO;
import item.dto.SpecParamDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spec")
public class SpecController {

    @Autowired
    private SpecService specService;

    /**
     查询规格组
     */
    @GetMapping("/groups/of/category")
    public ResponseEntity<List<SpecGroupDTO>> findSpecGroupByCid(@RequestParam("id") Long id){
        List<SpecGroupDTO> list = specService.findSpecGroupByCid(id);
        return ResponseEntity.ok(list);
    }

    /**
     * GET /spec/params?gid=1
     */
    @GetMapping("/params")
    public ResponseEntity<List<SpecParamDTO>> findSpecParam(
            @RequestParam(value = "gid",required = false)Long gid,
            @RequestParam(value = "cid",required = false)Long cid,
            @RequestParam(value = "searching",required = false)Boolean searching
    ){
        List<SpecParamDTO> list = specService.findSpecParam(gid,cid,searching);

        return ResponseEntity.ok(list);
    }

    /**
     * GET /spec/of/category?id=4
     * 根据categoryId查询规格参数组和组内参数
     */
    @GetMapping("/of/category")
    public ResponseEntity<List<SpecGroupDTO>> findSpecGroupAndParamByCid(@RequestParam("id") Long id){
        List<SpecGroupDTO> specGroupDTOS = specService.findSpecGroupAndParamByCid(id);
        return ResponseEntity.ok(specGroupDTOS);
    }

}
