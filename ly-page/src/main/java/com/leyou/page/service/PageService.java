package com.leyou.page.service;

import com.leyou.item.client.ItemClient;
import item.dto.BrandDTO;
import item.dto.CategoryDTO;
import item.dto.SpecGroupDTO;
import item.dto.SpuDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PageService {

    @Autowired
    private ItemClient itemClient;

    //模板引擎
    @Autowired
    private TemplateEngine templateEngine;

    @Value("${ly.static.itemDir}")
    private String itemDir;

    @Value("${ly.static.itemTemplate}")
    private String itemTemplate;


    public Map<String, Object> findItemData(Long id) {
        //现根据spuid查询出spu对象
        SpuDTO spuDTO = itemClient.findSpuById(id);
        //根据spuid查询出三级分类的对象集合
        List<CategoryDTO> categoryDTOS = itemClient.findCategoryByIds(spuDTO.getCategoryIds());
        //查询品牌对象
        BrandDTO brandDTO = itemClient.findBrandById(spuDTO.getBrandId());
        //根据spu对象中分类id查询规格组和规格参数的集合
        List<SpecGroupDTO> specGroupDTOS = itemClient.findSpecGroupAndParamByCid(spuDTO.getCid3());

        Map<String, Object> map = new HashMap<>();
        map.put("categories", categoryDTOS);//三级分类的对象集合
        map.put("brand", brandDTO);//品牌对象
        map.put("spuName", spuDTO.getName());//来自于SpuDTO
        map.put("subTitle", spuDTO.getSubTitle());//来自于SpuDTO
        map.put("detail", spuDTO.getSpuDetail());//SpuDetail对象  可以直接封装到SpuDTO对象中
        map.put("skus", spuDTO.getSkus());//Sku集合  可以直接封装到SpuDTO对象中
        map.put("specs", specGroupDTOS);//规格组集合，每个规格组中有个规格参数的集合
        return map;
    }

    //编写一个创建静态页面的方法
    public void createStaticItemPage(Long  id){

        //准备上下文数据
        Context context = new Context();
        context.setVariables(findItemData(id));

        //指定静态页面路径
        File file = new File(new File(itemDir),id+".html");
        try (PrintWriter writer = new PrintWriter(file)){
            templateEngine.process(itemTemplate,context,writer);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //编写一个删除静态页面的方法
    public void deleteStaticPage(Long id){
        //指定静态页面路径
        File file = new File(new File(itemDir),id+".html");
        if (file.exists()){
            file.delete();
        }
    }

}
