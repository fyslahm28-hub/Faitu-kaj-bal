package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiSeekerBlue
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.viewmodel.UiState

@Composable
fun PublishPostScreen(
    isCircular: Boolean,
    userProfileState: UiState<UserProfile?>,
    isSubmitting: Boolean,
    onSubmit: (
        authorName: String,
        postType: String,
        title: String,
        district: String,
        upazilaOrLocation: String,
        designation: String,
        qualification: String,
        salaryOrExperience: String,
        deadlineOrPreferredDistricts: String,
        description: String,
        contactNumber: String
    ) -> Unit
) {
    val existingProfile = (userProfileState as? UiState.Success)?.data

    var title by remember {
        mutableStateOf(
            if (isCircular) existingProfile?.madrasaName.orEmpty() else existingProfile?.fullName.orEmpty()
        )
    }
    var district by remember { mutableStateOf(existingProfile?.district.orEmpty()) }
    var upazilaOrLocation by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf(existingProfile?.designation.orEmpty()) }
    var qualification by remember { mutableStateOf(existingProfile?.qualification.orEmpty()) }
    var salaryOrExperience by remember {
        mutableStateOf(
            if (isCircular) "" else existingProfile?.experience.orEmpty()
        )
    }
    var deadlineOrPreferredDistricts by remember {
        mutableStateOf(
            if (isCircular) "" else existingProfile?.preferredDistricts.orEmpty()
        )
    }
    var description by remember { mutableStateOf(existingProfile?.bio.orEmpty()) }
    var contactNumber by remember { mutableStateOf(existingProfile?.contactInfo.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag(if (isCircular) "publish_circular_screen" else "publish_application_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCircular) Icons.Default.Campaign else Icons.Default.EditNote,
                    contentDescription = null,
                    tint = QawmiDeepGreen,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isCircular) "মাদরাসার খেদমত / নিয়োগ বিজ্ঞপ্তি প্রকাশ" else "খেদমতপ্রার্থীর আবেদন প্রকাশ করুন",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCircular) {
                            "আপনার মাদরাসার শূন্য পদের বিস্তারিত তথ্য দিন, যাতে যোগ্য আলিম ও মুদাররিসগণ আবেদন করতে পারেন।"
                        } else {
                            "আপনার ইলমি যোগ্যতা, তাজরিবা ও পছন্দের জেলা উল্লেখ করুন যাতে মাদরাসা কর্তৃপক্ষ যোগাযোগ করতে পারেন।"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(if (isCircular) "মাদরাসার নাম *" else "খেদমতপ্রার্থীর পূর্ণ নাম *")
                    },
                    placeholder = {
                        Text(if (isCircular) "যেমন: দারুল উলুম কাওমি মাদরাসা" else "যেমন: মাওলানা রফিকুল ইসলাম")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_title_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("জেলা *") },
                        placeholder = { Text("যেমন: কুমিল্লা") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("post_district_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = upazilaOrLocation,
                        onValueChange = { upazilaOrLocation = it },
                        label = { Text("উপজেলা / এলাকা") },
                        placeholder = { Text("যেমন: সদর দক্ষিণ") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("post_location_input"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text(if (isCircular) "খেদমতের পদ / বিভাগ *" else "কাঙ্ক্ষিত খেদমতের পদ *") },
                    placeholder = { Text("যেমন: সহকারী শিক্ষক (আরবি, ফিকহ, তাফসীর) / মুহতামিম") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_designation_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("শিক্ষাগত / ইলমি যোগ্যতা *") },
                    placeholder = { Text("যেমন: দাওরায়ে হাদীস / ইফতা / তাফসীর / মাস্টার্স") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_qualification_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = salaryOrExperience,
                    onValueChange = { salaryOrExperience = it },
                    label = {
                        Text(if (isCircular) "মাসিক হাদিয়া / বেতন" else "খেদমতের তাজরিবা (অভিজ্ঞতা)")
                    },
                    placeholder = {
                        Text(if (isCircular) "যেমন: ১২,০০০ - ১৮,০০০ টাকা / আলোচনা সাপেক্ষ" else "যেমন: ৫ বছর / নবীণ")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_salary_or_exp_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = deadlineOrPreferredDistricts,
                    onValueChange = { deadlineOrPreferredDistricts = it },
                    label = {
                        Text(if (isCircular) "আবেদনের শেষ তারিখ" else "পছন্দের জেলাসমূহ")
                    },
                    placeholder = {
                        Text(if (isCircular) "যেমন: ২০ নভেম্বর ২০২৬" else "যেমন: নোয়াখালী, লক্ষ্মীপুর, চট্টগ্রাম")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_deadline_or_pref_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("যোগাযোগের মোবাইল নম্বর *") },
                    placeholder = { Text("যেমন: ০১৭০০-০০০০০০") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_contact_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = {
                        Text(if (isCircular) "বিস্তারিত বিবরণ ও সুযোগ-সুবিধা (আবাসন, খানা ইত্যাদি)" else "নিজের ইলমি দক্ষতা ও বিস্তারিত বিবরণ")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("post_description_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        onSubmit(
                            title,
                            if (isCircular) "JOB_CIRCULAR" else "SEEKER_APPLICATION",
                            title,
                            district,
                            upazilaOrLocation,
                            designation,
                            qualification,
                            salaryOrExperience,
                            deadlineOrPreferredDistricts,
                            description,
                            contactNumber
                        )
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_post_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCircular) QawmiDeepGreen else QawmiSeekerBlue
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("প্রকাশ করা হচ্ছে...")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCircular) "বিজ্ঞপ্তি প্রকাশ করুন" else "আবেদন ফিডে প্রকাশ করুন",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterMadrasaScreen(
    isSubmitting: Boolean,
    onSubmit: (
        name: String,
        district: String,
        address: String,
        establishedYear: String,
        highestMarhala: String,
        facilities: String,
        studentCount: String,
        contactPhone: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var establishedYear by remember { mutableStateOf("") }
    var highestMarhala by remember { mutableStateOf("দাওরায়ে হাদীস (তাকমীল)") }
    var facilities by remember { mutableStateOf("উস্তাযদের মানসম্মত আবাসন, তিন বেলা খানা ও সমৃদ্ধ কুতুবখানা") }
    var studentCount by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("register_madrasa_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = QawmiDeepGreen,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "মাদরাসা নিবন্ধন করুন",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "কওমি মাদরাসা ডিরেক্টরিতে আপনার মাদরাসার তথ্য, অবস্থান ও বিশেষ সুবিধা যুক্ত করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("মাদরাসার পূর্ণ নাম *") },
                    placeholder = { Text("যেমন: জামিয়া ইসলামিয়া দারুল উলুম") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("madrasa_name_input"),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("জেলা *") },
                        placeholder = { Text("যেমন: সিলেট") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("madrasa_district_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = establishedYear,
                        onValueChange = { establishedYear = it },
                        label = { Text("প্রতিষ্ঠাকাল (সন)") },
                        placeholder = { Text("যেমন: ১৯৮৫ ঈসাব্দ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("বিস্তারিত ঠিকানা ও অবস্থান") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = highestMarhala,
                    onValueChange = { highestMarhala = it },
                    label = { Text("সর্বোচ্চ মারহালা / বিভাগসমূহ") },
                    placeholder = { Text("যেমন: ইফতা, দাওরায়ে হাদীস, হিফযুল কুরআন") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = studentCount,
                        onValueChange = { studentCount = it },
                        label = { Text("ছাত্র/তালিবে ইলম সংখ্যা") },
                        placeholder = { Text("যেমন: ৫০০ জন") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("দাপ্তরিক যোগাযোগ নম্বর") },
                        placeholder = { Text("০১৮০০-০০০০০০") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = facilities,
                    onValueChange = { facilities = it },
                    label = { Text("বিশেষ সুবিধা ও বৈশিষ্ট্য") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                )

                Button(
                    onClick = {
                        onSubmit(
                            name,
                            district,
                            address,
                            establishedYear,
                            highestMarhala,
                            facilities,
                            studentCount,
                            contactPhone
                        )
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_madrasa_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen)
                ) {
                    Text("মাদরাসা নিবন্ধন সম্পন্ন করুন", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    userProfileState: UiState<UserProfile?>,
    currentUserDisplayName: String,
    currentUserEmail: String,
    isSubmitting: Boolean,
    onSaveProfile: (
        fullName: String,
        accountType: String,
        district: String,
        designation: String,
        qualification: String,
        experience: String,
        preferredDistricts: String,
        bio: String,
        contactInfo: String,
        madrasaName: String
    ) -> Unit
) {
    val profile = (userProfileState as? UiState.Success)?.data

    var fullName by remember { mutableStateOf(profile?.fullName ?: currentUserDisplayName) }
    var accountType by remember { mutableStateOf(profile?.accountType ?: "khadim") }
    var district by remember { mutableStateOf(profile?.district ?: "") }
    var designation by remember { mutableStateOf(profile?.designation ?: "") }
    var qualification by remember { mutableStateOf(profile?.qualification ?: "") }
    var experience by remember { mutableStateOf(profile?.experience ?: "") }
    var preferredDistricts by remember { mutableStateOf(profile?.preferredDistricts ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }
    var contactInfo by remember { mutableStateOf(profile?.contactInfo ?: "") }
    var madrasaName by remember { mutableStateOf(profile?.madrasaName ?: "") }

    LaunchedEffect(profile) {
        if (profile != null) {
            fullName = profile.fullName
            accountType = profile.accountType
            district = profile.district
            designation = profile.designation
            qualification = profile.qualification
            experience = profile.experience
            preferredDistricts = profile.preferredDistricts
            bio = profile.bio
            contactInfo = profile.contactInfo
            madrasaName = profile.madrasaName
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = QawmiMintSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "আমার প্রোফাইল ও ইলমি পরিচিতি",
                    style = MaterialTheme.typography.titleMedium,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ইমেইল: ${currentUserEmail.ifBlank { "সংযুক্ত গুগল অ্যাকাউন্ট" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = QawmiTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = accountType == "khadim",
                        onClick = { accountType = "khadim" },
                        label = { Text("খেদমতপ্রার্থী / মুদাররিস") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QawmiDeepGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = accountType == "muhtamim",
                        onClick = { accountType = "muhtamim" },
                        label = { Text("মুহতামিম / মাদরাসা কর্তৃপক্ষ") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QawmiDeepGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QawmiMintBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("পূর্ণ নাম *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("নিজ জেলা") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("পদবী / খেদমতের ক্ষেত্র") },
                    placeholder = { Text("যেমন: মুহাদ্দিস / সহকারী শিক্ষক / মুহতামিম") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("শিক্ষাগত / ইলমি যোগ্যতা") },
                    placeholder = { Text("যেমন: দাওরায়ে হাদীস (বেফাক), ইফতা") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = experience,
                    onValueChange = { experience = it },
                    label = { Text("খেদমতের অভিজ্ঞতা (তাজরিবা)") },
                    placeholder = { Text("যেমন: ৪ বছর দারস প্রদানের অভিজ্ঞতা") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = preferredDistricts,
                    onValueChange = { preferredDistricts = it },
                    label = { Text("পছন্দের জেলাসমূহ") },
                    placeholder = { Text("যেমন: ঢাকা, কুমিল্লা, নোয়াখালী") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = madrasaName,
                    onValueChange = { madrasaName = it },
                    label = { Text("বর্তমান / সংশ্লিষ্ট মাদরাসার নাম (থাকলে)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("যোগাযোগের মোবাইল নম্বর") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("সংক্ষিপ্ত পরিচিতি ও দক্ষতার বিবরণ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Button(
                    onClick = {
                        onSaveProfile(
                            fullName,
                            accountType,
                            district,
                            designation,
                            qualification,
                            experience,
                            preferredDistricts,
                            bio,
                            contactInfo,
                            madrasaName
                        )
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QawmiDeepGreen)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("প্রোফাইল আপডেট করুন", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
