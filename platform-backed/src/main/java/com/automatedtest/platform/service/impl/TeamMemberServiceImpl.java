package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.TeamMember;
import com.automatedtest.platform.mapper.TeamMemberMapper;
import com.automatedtest.platform.service.TeamMemberService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TeamMemberServiceImpl extends ServiceImpl<TeamMemberMapper, TeamMember> implements TeamMemberService {
}