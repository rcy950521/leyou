package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.domain.SpecGroup;
import com.leyou.item.domain.SpecParam;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import item.dto.SpecGroupDTO;
import item.dto.SpecParamDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpecService {

    @Autowired
    private SpecGroupMapper specGroupMapper;

    @Autowired
    private SpecParamMapper specParamMapper;

    /**
     * 根据cid查询规格组信息
     * @param id
     * @return
     */
    public List<SpecGroupDTO> findSpecGroupByCid(Long id) {

        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(id);

        List<SpecGroup> specGroups = specGroupMapper.select(specGroup);

        if (CollectionUtils.isEmpty(specGroups)){
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }

        return BeanHelper.copyWithCollection(specGroups,SpecGroupDTO.class);
    }

    /**
     * 查询规格参数
     * @param gid
     * @param cid
     * @param searching
     * @return
     */
    public List<SpecParamDTO> findSpecParam(Long gid, Long cid, Boolean searching) {

        //要求规格组id或者分类id必须要有一个
        if (cid==null&&gid==null){
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }

        SpecParam specParam = new SpecParam();
        specParam.setCid(cid);
        specParam.setGroupId(gid);
        specParam.setSearching(searching);

        List<SpecParam> specParams = specParamMapper.select(specParam);

        if (CollectionUtils.isEmpty(specParams)){
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }

        return BeanHelper.copyWithCollection(specParams,SpecParamDTO.class);
    }

    /**
     * 根据categoryId查询规格参数组和组内参数
     * @param id
     * @return
     */
    public List<SpecGroupDTO> findSpecGroupAndParamByCid(Long id) {
        //获取规格参数组
        List<SpecGroupDTO> specGroupDTOS = findSpecGroupByCid(id);

        //获取规格参数
        List<SpecParamDTO> specParams = findSpecParam(null, id, null);

        //根据规格组id 给规格分类
        Map<Long, List<SpecParamDTO>> paramsMap = specParams.stream().collect(Collectors.groupingBy(SpecParamDTO::getGroupId));

        specGroupDTOS.forEach(specGroupDTO -> {
            specGroupDTO.setParams(paramsMap.get(specGroupDTO.getId()));
        });

        return specGroupDTOS;
    }
}
