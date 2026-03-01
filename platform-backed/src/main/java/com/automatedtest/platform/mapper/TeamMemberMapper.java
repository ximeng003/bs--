package com.automatedtest.platform.mapper;

import com.automatedtest.platform.entity.TeamMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {
}