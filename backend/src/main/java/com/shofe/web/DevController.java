package com.shofe.web;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.enums.VerificationStatus;
import com.shofe.dto.MiscDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.DriverProfileRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevController {

    private final DriverProfileRepository drivers;

    @Value("${shofe.dev-tools}")
    private boolean devTools;

    @PostMapping("/teleport-drivers")
    @Transactional
    public int teleport(@Valid @RequestBody MiscDtos.TeleportRequest request) {
        if (!devTools) {
            throw ApiException.notFound("Not found");
        }
        List<DriverProfile> online = drivers.findByOnlineTrueAndVerificationStatus(VerificationStatus.APPROVED);
        int index = 0;
        for (DriverProfile profile : online) {
            double shift = 0.004 * (index + 1);
            profile.setLastLat(request.lat() + (index % 2 == 0 ? shift : -shift));
            profile.setLastLng(request.lng() + shift / 2);
            profile.setOnline(true);
            index++;
        }
        return online.size();
    }
}
