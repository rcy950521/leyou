package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.constants.MQConstants;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.domain.Sku;
import com.leyou.item.domain.Spu;
import com.leyou.item.domain.SpuDetail;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import item.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.chrono.IslamicChronology;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 分页显示spu
     *
     * @param page
     * @param rows
     * @param key
     * @param saleable
     * @return
     */
    public PageResult<SpuDTO> findSpuPage(Integer page, Integer rows, String key, Boolean saleable) {

        PageHelper.startPage(page, rows);

        Example example = new Example(Spu.class);

        Example.Criteria criteria = example.createCriteria();

        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("name", "%" + key + "%");
        }
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }

        List<Spu> spus = spuMapper.selectByExample(example);

        PageInfo<Spu> pageInfo = new PageInfo<>(spus);

        List<SpuDTO> spuDTOS = BeanHelper.copyWithCollection(pageInfo.getList(), SpuDTO.class);

        //给categoryName和brandName 赋值
        handlerCategoryNameAndBrandName(spuDTOS);

        return new PageResult(pageInfo.getTotal(), pageInfo.getPages(), spuDTOS);
    }


    //给categoryName和brandName 赋值
    private void handlerCategoryNameAndBrandName(List<SpuDTO> spuDTOS) {
        spuDTOS.forEach(spuDTO -> {
            String categoryNames = categoryService.findCategoryByIds(spuDTO.getCategoryIds())
                    .stream()
                    .map(CategoryDTO::getName)
                    .collect(Collectors.joining("/"));
            spuDTO.setCategoryName(categoryNames);

            BrandDTO brandDTO = brandService.findBrandById(spuDTO.getBrandId());
            spuDTO.setBrandName(brandDTO.getName());
        });

    }


    /**
     * 添加商品
     *
     * @param spuDTO
     */
    public void saveGoods(SpuDTO spuDTO) {
        try {
            //1.将spudto转换成spu
            Spu spu = BeanHelper.copyProperties(spuDTO, Spu.class);
            //将起始的状态设为下架
            spu.setSaleable(false);
            spu.setCreateTime(new Date());
            spu.setUpdateTime(new Date());
            //保存spu
            spuMapper.insert(spu);

            //2.保存spudetail
            //获得spuDetail
            SpuDetailDTO spuDetailDTO = spuDTO.getSpuDetail();
            //将Dto转为pojo
            SpuDetail spuDetail = BeanHelper.copyProperties(spuDetailDTO, SpuDetail.class);
            spuDetail.setSpuId(spu.getId());
            spuDetail.setCreateTime(new Date());
            spuDetail.setUpdateTime(new Date());
            //保存商品详情
            spuDetailMapper.insert(spuDetail);

            //3.保存sku
            //获得sku
            List<SkuDTO> skus = spuDTO.getSkus();
            //将skudto转为pojo对象
            List<Sku> skuList = BeanHelper.copyWithCollection(skus, Sku.class);
            //保存sku
            skuList.forEach(sku -> {
                //将spu的id保存到sku中
                sku.setSpuId(spu.getId());
                sku.setCreateTime(new Date());
                sku.setUpdateTime(new Date());
            });
            skuMapper.insertList(skuList);
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }

    }

    /**
     * 商品上下架
     * @param id
     * @param saleable
     */
    public void updateSaleable(Long id, Boolean saleable) {
        //修改spu中的saleable字段
        try {
            Spu spu = new Spu();
            spu.setId(id);
            spu.setSaleable(saleable);
            spuMapper.updateByPrimaryKeySelective(spu);

            //商品上下架引起索引库变化和静态详情页的增删是采用异步消息的方式实现的
            String routingKey = saleable?MQConstants.RoutingKey.ITEM_UP_KEY:MQConstants.RoutingKey.ITEM_DOWN_KEY;
            //参数：交换机 routingKey，消息
            amqpTemplate.convertAndSend(MQConstants.Exchange.ITEM_EXCHANGE_NAME,routingKey,id);
        }catch (Exception e){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

    }

    /**
     * 根据spu id查询详细信息
     * @param id
     * @return
     */
    public SpuDetailDTO findSpuDetail(Long id) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(id);
        if (spuDetail == null){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
        return BeanHelper.copyProperties(spuDetail,SpuDetailDTO.class);
    }

    /**根据spuid查询sku
     *
     * @param id
     * @return
     */
    public List<SkuDTO> findSkuBySpuId(Long id) {
        Sku sku = new Sku();
        sku.setSpuId(id);
        List<Sku> skuList = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
        return BeanHelper.copyWithCollection(skuList,SkuDTO.class);
    }

    /**
     * 修改商品
     * @param spuDTO
     */
    public void updateGoods(SpuDTO spuDTO) {
        try {
            //修改商品
            //1.修改spudto
            Spu spu = BeanHelper.copyProperties(spuDTO, Spu.class);
            spuMapper.updateByPrimaryKeySelective(spu);

            //2.修改spudetail
            SpuDetailDTO spuDetailDTO = spuDTO.getSpuDetail();
            SpuDetail spuDetail = BeanHelper.copyProperties(spuDetailDTO, SpuDetail.class);
            spuDetailMapper.updateByPrimaryKeySelective(spuDetail);
            //3.修改sku
            //3.1 先根据spuid删除对应的所有sku
            Sku record = new Sku();
            record.setSpuId(spu.getId());
            skuMapper.delete(record);
            List<SkuDTO> skuDTOS = spuDTO.getSkus();
            List<Sku> skus = BeanHelper.copyWithCollection(skuDTOS, Sku.class);
            skus.forEach(sku->{
                sku.setSpuId(spu.getId());
                sku.setCreateTime(new Date());
                sku.setUpdateTime(new Date());
            });
            skuMapper.insertList(skus);
        }catch (Exception e){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
    }

    /**
     * 根据spuid查询spu信息
     * @param id
     * @return
     */
    public SpuDTO findSpuById(Long id) {
        //获得spu
        Spu spu = spuMapper.selectByPrimaryKey(id);

        if (spu==null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        SpuDTO spuDTO = BeanHelper.copyProperties(spu, SpuDTO.class);

        //获取spudetail对象
        SpuDetailDTO spuDetailDTO = findSpuDetail(id);
        spuDTO.setSpuDetail(spuDetailDTO);

        //获取sku对象
        List<SkuDTO> skus = findSkuBySpuId(id);
        spuDTO.setSkus(skus);

        return spuDTO;
    }
}
