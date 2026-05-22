package com.puyuanmaoshan.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puyuanmaoshan.platform.dto.SupplierDtos;
import com.puyuanmaoshan.platform.entity.SupplierRegistration;
import com.puyuanmaoshan.platform.entity.Tenant;
import com.puyuanmaoshan.platform.entity.UserAccount;
import com.puyuanmaoshan.platform.mapper.SupplierRegistrationMapper;
import com.puyuanmaoshan.platform.service.SupplierRegistrationService;
import com.puyuanmaoshan.platform.service.TenantMemberService;
import com.puyuanmaoshan.platform.service.TenantService;
import com.puyuanmaoshan.platform.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierRegistrationServiceImpl extends ServiceImpl<SupplierRegistrationMapper, SupplierRegistration>
    implements SupplierRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierRegistrationServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final TenantService tenantService;
    private final UserAccountService userAccountService;
    private final TenantMemberService tenantMemberService;

    public SupplierRegistrationServiceImpl(ObjectMapper objectMapper,
                                           TenantService tenantService,
                                           UserAccountService userAccountService,
                                           TenantMemberService tenantMemberService) {
        this.objectMapper = objectMapper;
        this.tenantService = tenantService;
        this.userAccountService = userAccountService;
        this.tenantMemberService = tenantMemberService;
    }

    @Override
    @Transactional
    public SupplierDtos.RegisterResponse register(SupplierDtos.RegisterRequest request) {
        try {
            // 检查手机号是否已注册
            SupplierRegistration existing = this.getOne(new LambdaQueryWrapper<SupplierRegistration>()
                .eq(SupplierRegistration::getContactMobile, request.contactMobile())
                .eq(SupplierRegistration::getStatus, "approved"));

            if (existing != null) {
                throw new RuntimeException("该手机号已入驻");
            }

            SupplierRegistration registration = SupplierRegistration.builder()
                .companyName(request.companyName())
                .contactName(request.contactName())
                .contactMobile(request.contactMobile())
                .businessLicense(request.businessLicense())
                .address(request.address())
                .fabricCategories(request.fabricCategories() != null ?
                    objectMapper.writeValueAsString(request.fabricCategories()) : "[]")
                .description(request.description())
                .status("pending")
                .createdAt(LocalDateTime.now())
                .build();

            this.save(registration);

            return new SupplierDtos.RegisterResponse(registration.getId(), "pending");

        } catch (Exception e) {
            logger.error("Supplier registration failed", e);
            throw new RuntimeException("入驻申请提交失败: " + e.getMessage());
        }
    }

    @Override
    public SupplierDtos.RegistrationListResponse getRegistrations(int page, int size) {
        try {
            LambdaQueryWrapper<SupplierRegistration> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(SupplierRegistration::getCreatedAt);

            long total = this.count(wrapper);
            int offset = (page - 1) * size;
            wrapper.last("LIMIT " + offset + ", " + size);

            List<SupplierRegistration> registrations = this.list(wrapper);

            List<SupplierDtos.RegistrationItem> items = registrations.stream().map(reg -> {
                try {
                    List<String> categories = objectMapper.readValue(reg.getFabricCategories(),
                        new TypeReference<List<String>>() {});
                    return new SupplierDtos.RegistrationItem(
                        reg.getId(), reg.getCompanyName(), reg.getContactName(),
                        reg.getContactMobile(), categories, reg.getStatus(), reg.getCreatedAt()
                    );
                } catch (Exception e) {
                    logger.error("Parse fabric categories failed", e);
                    return new SupplierDtos.RegistrationItem(
                        reg.getId(), reg.getCompanyName(), reg.getContactName(),
                        reg.getContactMobile(), List.of(), reg.getStatus(), reg.getCreatedAt()
                    );
                }
            }).collect(Collectors.toList());

            return new SupplierDtos.RegistrationListResponse(items, total);

        } catch (Exception e) {
            logger.error("Get registrations failed", e);
            throw new RuntimeException("获取入驻申请列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SupplierDtos.ReviewResponse reviewRegistration(Long registrationId,
                                                          SupplierDtos.ReviewRequest request,
                                                          Long adminId) {
        try {
            SupplierRegistration registration = this.getById(registrationId);
            if (registration == null) {
                throw new RuntimeException("入驻申请不存在");
            }

            if (!"pending".equals(registration.getStatus())) {
                throw new RuntimeException("该申请已处理");
            }

            if ("approve".equals(request.action())) {
                // 创建租户
                Tenant tenant = Tenant.builder()
                    .tenantCode("SUP-" + System.currentTimeMillis())
                    .name(registration.getCompanyName())
                    .status(1)
                    .level("basic")
                    .tenantType("supplier")
                    .createdAt(LocalDateTime.now())
                    .build();
                tenantService.save(tenant);

                // 创建用户账号
                UserAccount user = UserAccount.builder()
                    .tenantId(tenant.getId())
                    .mobile(registration.getContactMobile())
                    .nickname(registration.getContactName())
                    .roleCode("boss")
                    .status(1)
                    .createdAt(LocalDateTime.now())
                    .build();
                userAccountService.save(user);

                // 创建租户用户关联
                tenantMemberService.saveTenantUser(tenant.getId(), user.getId(), "boss", 0L);

                // 更新申请状态
                registration.setStatus("approved");
                registration.setTenantId(tenant.getId());
                registration.setUserId(user.getId());
                registration.setAdminId(adminId);
                registration.setReviewedAt(LocalDateTime.now());
                this.updateById(registration);

                return new SupplierDtos.ReviewResponse(tenant.getId(), user.getId(), "审核通过，已自动创建账号");

            } else if ("reject".equals(request.action())) {
                registration.setStatus("rejected");
                registration.setRejectReason(request.rejectReason());
                registration.setAdminId(adminId);
                registration.setReviewedAt(LocalDateTime.now());
                this.updateById(registration);

                return new SupplierDtos.ReviewResponse(0L, 0L, "已驳回申请");

            } else {
                throw new RuntimeException("无效的审核操作");
            }

        } catch (Exception e) {
            logger.error("Review registration failed", e);
            throw new RuntimeException("审核失败: " + e.getMessage());
        }
    }
}