package com.nineplus.bestwork.entity;

import java.sql.Timestamp;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nineplus.bestwork.model.enumtype.Status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SYS_PERMISSION")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SysPermissionEntity implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", unique = true, nullable = false, columnDefinition = "bigint")
	private Long id;

	@Column(name = "can_access", nullable = false, columnDefinition = "tinyint(1)")
	private boolean canAccess;

	@Column(name = "can_add", nullable = false, columnDefinition = "tinyint(1)")
	private boolean canAdd;

	@Column(name = "can_edit", nullable = false, columnDefinition = "tinyint(1)")
	private boolean canEdit;

	@Column(name = "can_delete", nullable = false, columnDefinition = "tinyint(1)")
	private boolean canDelete;

	@Column(name = "created_user", nullable = false, columnDefinition = "varchar(20)")
	private String createdUser;

	@Column(name = "created_date", nullable = false)
	private Timestamp createdDate;

	@Column(name = "updated_user", columnDefinition = "varchar(20)")
	private String updatedUser;

	@Column(name = "updated_date")
	private Timestamp updatedDate;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	Status status;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "monitor_id")
	@JsonManagedReference
	private SysMonitorEntity sysMonitor;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "role_id")
	@JsonManagedReference
	private RoleEntity sysRole;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "admin_id")
	@JsonManagedReference
	private UserEntity user;

	public Integer getStatus() {
		return status.getValue();
	}

	public void setStatus(Integer status) {
		this.status = Status.fromValue(status);
	}

	@Override
	public SysPermissionEntity clone() {
		try {
			// TODO: copy mutable state here, so the clone can't change the internals of the original
			return (SysPermissionEntity) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
