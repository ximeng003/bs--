package com.automatedtest.platform.service.impl;

import com.automatedtest.platform.entity.Team;
import com.automatedtest.platform.mapper.TeamMapper;
import com.automatedtest.platform.service.TeamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {
}