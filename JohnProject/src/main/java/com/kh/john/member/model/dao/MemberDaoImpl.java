package com.kh.john.member.model.dao;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.kh.john.member.model.vo.License;
import com.kh.john.member.model.vo.Member;

@Repository
public class MemberDaoImpl implements MemberDao {

	@Override
	public List<Map<String, Object>> selectMember(SqlSessionTemplate session) {
		// TODO Auto-generated method stub
		return session.selectList("member.selectMember");
	}
	
	@Override
	public Member selectMemberById(SqlSessionTemplate session, Member member) {
		// TODO Auto-generated method stub
		return session.selectOne("member.selectMemberById",member);
	}
	@Override
	public Member selectMemberById(SqlSessionTemplate session, Map param) {
		return session.selectOne("member.selectMemberById",param);
	}

	@Override
	public Member nickDuplicate(SqlSessionTemplate session, Member member) {
		return session.selectOne("member.selectMemberByNick", member);
	}

	@Override
	public Member phoneDuplicate(SqlSessionTemplate session, Member member) {
		return session.selectOne("member.selectMemberByPhone", member);
	}
	
	@Override
	public int signUpEnd(SqlSessionTemplate session, Member member) {
		return session.insert("member.signUpEnd",member);
	}

	@Override
	public int signUpExpert(SqlSessionTemplate session, License l) {
		return session.insert("member.signUpExpert",l);
	}

}
