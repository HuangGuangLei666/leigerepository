package com.qianfeng.oa.mapper;

import com.qianfeng.oa.entity.TGroupTag;

public interface TGroupTagMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TGroupTag record);

    int insertSelective(TGroupTag record);

    TGroupTag selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TGroupTag record);

    int updateByPrimaryKey(TGroupTag record);
}