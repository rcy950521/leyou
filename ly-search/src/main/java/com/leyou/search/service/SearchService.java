package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.BeanHelper;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.client.ItemClient;
import com.leyou.search.domain.Goods;
import com.leyou.search.domain.SearchRequest;
import com.leyou.search.repository.SearchRepository;
import item.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private ItemClient itemClient;

    @Autowired
    private ElasticsearchTemplate esTemplate;

    @Autowired
    private SearchRepository searchRepository;

    /**
     * 将一个spu 对象转换成Goods对象
     *
     * @param spuDTO
     * @return
     */
    public Goods buildGoods(SpuDTO spuDTO) {

        //查询sku 根据spuid
        List<SkuDTO> skuDTOList = itemClient.findSkuBySpuId(spuDTO.getId());
        //创建一个list集合，里面只有四个属性 id title price image
        List<Map<String, Object>> skuList = new ArrayList<>();
        //遍历skudtolist 给maplist 赋值
        skuDTOList.forEach(skuDTO -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", skuDTO.getId());
            map.put("title", skuDTO.getTitle());
            map.put("price", skuDTO.getPrice());
            map.put("image", StringUtils.substringBefore(skuDTO.getImages(), ","));
            skuList.add(map);
        });

        //获得价格数据
        Set<Long> price = skuDTOList.stream().map(SkuDTO::getPrice).collect(Collectors.toSet());

        //获取规格参数的key所在的对象
        List<SpecParamDTO> specParamDTOList = itemClient.findSpecParam(null, spuDTO.getCid3(), true);
        //获取value所在的对象
        SpuDetailDTO spuDetailDTO = itemClient.findSpuDetail(spuDTO.getId());
        //获取全局通用规格
        String genericSpec = spuDetailDTO.getGenericSpec();
        //将数据转为map
        Map<Long, Object> genericSpecMap = JsonUtils.toMap(genericSpec, Long.class, Object.class);
        //获取特殊规格的参数
        String specialSpec = spuDetailDTO.getSpecialSpec();
        //将查询到的数据转为map
        Map<Long, List<String>> specialSpecMap = JsonUtils.nativeRead(specialSpec, new TypeReference<Map<Long, List<String>>>() {
        });

        //创建一个map 保存可搜索的规格参数
        Map<String, Object> specs = new HashMap<>();
        //遍历specParamDTOList 获取对应的key 和value
        specParamDTOList.forEach(specParamDTO -> {
            String key = specParamDTO.getName();
            Object value = null;
            //如果是普通属性
            if (specParamDTO.getGeneric()) {
                value = genericSpecMap.get(specParamDTO.getId());
            } else {
                value = specialSpecMap.get(specParamDTO.getId());
            }

            //判断规格信息是否是数字类型
            if (specParamDTO.getNumeric()) {
                value = chooseSegment(value, specParamDTO);
            }
            //将数据放入集合中
            specs.put(key, value);

        });

        Goods good = new Goods();

        good.setId(spuDTO.getId());
        good.setSubTitle(spuDTO.getSubTitle());
        good.setBrandId(spuDTO.getBrandId());
        good.setCategoryId(spuDTO.getCid3());
        good.setPrice(price);
        good.setSkus(JsonUtils.toString(skuList));
        good.setAll(spuDTO.getName() + spuDTO.getCategoryName() + spuDTO.getBrandName());
        good.setSpecs(specs);
        return good;
    }


    //将目前规格参数的值中所有为数字的都转成区间来存储到索引库
    private String chooseSegment(Object value, SpecParamDTO p) {
        if (value == null || StringUtils.isBlank(value.toString())) {
            return "其它";
        }
        double val = parseDouble(value.toString());
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = parseDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = parseDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 查询商城搜索
     * @param searchRequest
     * @return
     */
    public PageResult<GoodsDTO> goodsPageQuery(SearchRequest searchRequest) {

        //提供一个可以封装复杂条件的查询构造器
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        //指定要查询的字段域
        searchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","subTitle","skus"},null));

        //封装分页信息
        searchQueryBuilder.withPageable(PageRequest.of(searchRequest.getPage()-1,searchRequest.getSize()));

        //封装查询条件
        searchQueryBuilder.withQuery(buildSearchKey(searchRequest));
        AggregatedPage<Goods> goodsPageResult = esTemplate.queryForPage(searchQueryBuilder.build(), Goods.class);

        //得到分页中的goods对象
        List<Goods> goodsList = goodsPageResult.getContent();
        if (CollectionUtils.isEmpty(goodsList)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        return new PageResult<>(goodsPageResult.getTotalElements(),
                goodsPageResult.getTotalPages(),
                BeanHelper.copyWithCollection(goodsList,GoodsDTO.class));
    }

    /**
     * 构建查询条件
     */
    private QueryBuilder buildSearchKey(SearchRequest searchRequest){
        //创建一个组合条件查询器
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //向条件查询中加入必须查询的条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all",searchRequest.getKey()).operator(Operator.AND));

        //向条件查询加入其他查询条件
        //获取过滤参数
        Map< String, Object> filterParamMap = searchRequest.getFilterParams();

        //遍历集合
        filterParamMap.entrySet().forEach(entry->{
            String filed = entry.getKey();
            Object value = entry.getValue();
            if (filed.equals("分类")){
                 filed = "categoryId";
            }else if (filed.equals("品牌")){
                 filed = "brandId";
            }else {
                 filed = "specs."+filed;
            }
            boolQueryBuilder.filter(QueryBuilders.termsQuery(filed,value));
        });

        return boolQueryBuilder;
    }

    /**
     * 根据查询条件查询分类和品牌
     * @param searchRequest
     * @return
     */
    public Map<String, List<?>> filterParamQuery(SearchRequest searchRequest) {

        Map<String, List<?>> filterParamMap = new LinkedHashMap<>();

        //创建一个封装复杂条件的查询构造器
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        //指定要查询的字段域
        searchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""},null));

        //封装分页信息
        searchQueryBuilder.withPageable(PageRequest.of(0,1));

        //封装查询条件
        searchQueryBuilder.withQuery(buildSearchKey(searchRequest));

        //封装分类聚合条件
        String categoryAgg = "categoryAgg";
        searchQueryBuilder.addAggregation(AggregationBuilders.terms(categoryAgg).field("categoryId"));

        //封装品牌聚合条件
        String brandAgg = "brandAgg";
        searchQueryBuilder.addAggregation(AggregationBuilders.terms(brandAgg).field("brandId"));

        AggregatedPage<Goods> goodsPageResult = esTemplate.queryForPage(searchQueryBuilder.build(), Goods.class);

        //得到对应的对象
        Terms categoryTerms = (Terms) goodsPageResult.getAggregation(categoryAgg);
        //将结果封装到过滤对象中
        List<Long> categoryIds = handlerCategoryParam(categoryTerms, filterParamMap);

        //得到品牌的对象 并封装 到过滤对象中
        Terms brandTerms  = goodsPageResult.getAggregations().get(brandAgg);
        handlerBrandParam(brandTerms,filterParamMap);

        //将规格参数封装到过滤结果中
        addSpecParamInFilterParamMap(filterParamMap,categoryIds,buildSearchKey(searchRequest));

        return filterParamMap;
    }

    /**
     * 将规格参数封装到过滤结果中
     * @param filterParamMap
     * @param categoryIds
     * @param buildSearchKey
     */
    private void addSpecParamInFilterParamMap(Map<String, List<?>> filterParamMap, List<Long> categoryIds, QueryBuilder buildSearchKey) {
        //根据分类id获取规格参数
        categoryIds.forEach(categoryId->{
            //得到不同分类对应的规格参数
            List<SpecParamDTO> specParamDTOS = itemClient.findSpecParam(null, categoryId, true);

            //创建一个封装复杂条件的查询构造器
            NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

            //指定要查询的字段域
            searchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""},null));

            //封装分页信息
            searchQueryBuilder.withPageable(PageRequest.of(0,1));

            //封装查询条件
            searchQueryBuilder.withQuery(buildSearchKey);

            //封装规格聚合条件
            specParamDTOS.forEach(specParamDTO -> {
                //得到组合名称
                String aggName =specParamDTO.getName();
                //得到域名称
                String filedName = "specs." + aggName;

                searchQueryBuilder.addAggregation(AggregationBuilders.terms(aggName).field(filedName));
            });

            //索引库查询
            AggregatedPage<Goods> goodsAggregatedResult = esTemplate.queryForPage(searchQueryBuilder.build(), Goods.class);

            //得到所有的组合结果
            Aggregations aggregations = goodsAggregatedResult.getAggregations();

            //遍历解析出聚合结果
            specParamDTOS.forEach(specParamDTO -> {
                //得到组合名称
                String aggName = specParamDTO.getName();
                //根据聚合后桶的名字得到桶
                Terms terms = aggregations.get(aggName);
                //解析桶中数据的集合
                List<String> specList = terms.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                //将得到的规格参数集合放入到filterParamMap中
                filterParamMap.put(aggName, specList);
            });
        });
    }

    /**
     * 将品牌结果封装到过滤对象中
     * @param brandTerms
     * @param filterParamMap
     */
    private void handlerBrandParam(Terms brandTerms, Map<String, List<?>> filterParamMap) {
        List<BrandDTO> brandDTOS = brandTerms.getBuckets()
                .stream()
                .map(Terms.Bucket::getKeyAsNumber)
                .map(Number::longValue)
                .map(itemClient::findBrandById)
                .collect(Collectors.toList());
        filterParamMap.put("品牌",brandDTOS);
    }

    /**
     * 将分类结果封装到过滤对象中
     * @param categoryTerms
     * @param filterParamMap
     */
    private List<Long> handlerCategoryParam(Terms categoryTerms, Map<String, List<?>> filterParamMap) {
        List<Long> ids = categoryTerms.getBuckets()
                .stream()
                .map(Terms.Bucket::getKeyAsNumber)
                .map(Number::longValue)
                .collect(Collectors.toList());
        List<CategoryDTO> categoryDTOList = itemClient.findCategoryByIds(ids);
        filterParamMap.put("分类",categoryDTOList);
        return ids;
    }

    //增加索引
    public void addIndex(Long id) {
        SpuDTO spuDTO = itemClient.findSpuById(id);
        Goods goods = buildGoods(spuDTO);
        searchRepository.save(goods);
    }

    //删除索引
    public void delIndex(Long id) {
        searchRepository.deleteById(id);
    }
}
