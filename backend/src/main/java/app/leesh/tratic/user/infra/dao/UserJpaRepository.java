package app.leesh.tratic.user.infra.dao;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import app.leesh.tratic.user.infra.entity.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
}
