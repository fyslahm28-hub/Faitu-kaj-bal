package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookmarkItem
import com.example.data.model.FeedPost
import com.example.data.model.KhedmotSubmission
import com.example.data.model.UserProfile
import com.example.ui.components.IslamicStarEmblem
import com.example.ui.components.UserAvatarImage
import com.example.ui.theme.QawmiDarkGreen
import com.example.ui.theme.QawmiDeepGreen
import com.example.ui.theme.QawmiGold
import com.example.ui.theme.QawmiGoldLight
import com.example.ui.theme.QawmiMintBorder
import com.example.ui.theme.QawmiMintSurface
import com.example.ui.theme.QawmiSeekerBlue
import com.example.ui.theme.QawmiSoftBg
import com.example.ui.theme.QawmiTextDark
import com.example.ui.theme.QawmiTextMuted
import com.example.ui.theme.QawmiTextSecondary
import com.example.ui.theme.QawmiVerifiedGreen
import com.example.ui.util.IslamicDateAndPrayerHelper
import com.example.ui.util.ProfilePhotoHelper
import com.example.ui.viewmodel.MainDestination
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
    currentUserId: String,
    currentUserDisplayName: String,
    currentUserEmail: String,
    feedPostsState: UiState<List<FeedPost>> = UiState.Success(emptyList()),
    mySentSubmissionsState: UiState<List<KhedmotSubmission>> = UiState.Success(emptyList()),
    receivedSubmissionsState: UiState<List<KhedmotSubmission>> = UiState.Success(emptyList()),
    bookmarksState: UiState<List<BookmarkItem>> = UiState.Success(emptyList()),
    isSubmitting: Boolean,
    onUpdatePhotoImmediately: (String) -> Unit = {},
    onViewPostDetail: (FeedPost) -> Unit = {},
    onDeleteOwnPost: (String) -> Unit = {},
    onNavigate: (MainDestination) -> Unit = {},
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
        madrasaName: String,
        photoUrl: String
    ) -> Unit
) {
    val context = LocalContext.current
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
    var photoUrl by remember { mutableStateOf(profile?.photoUrl ?: "") }

    var isEditExpanded by remember { mutableStateOf(true) }
    var selectedActivityTab by remember { mutableIntStateOf(0) } // 0 = আমার প্রকাশিত পোস্ট, 1 = আবেদন ও কার্যক্রম

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
            photoUrl = profile.photoUrl
        }
    }

    // Zero-permission Android Photo Picker for Profile Picture
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encodedDataUrl = ProfilePhotoHelper.encodeUriToDataUrl(context, uri)
            if (!encodedDataUrl.isNullOrBlank()) {
                photoUrl = encodedDataUrl
                onUpdatePhotoImmediately(encodedDataUrl)
            }
        }
    }

    val myPublishedPosts = remember(feedPostsState, currentUserId) {
        (feedPostsState as? UiState.Success)?.data
            ?.filter { it.authorId == currentUserId }
            ?: emptyList()
    }
    val mySentSubmissions = remember(mySentSubmissionsState) {
        (mySentSubmissionsState as? UiState.Success)?.data ?: emptyList()
    }
    val myReceivedSubmissions = remember(receivedSubmissionsState) {
        (receivedSubmissionsState as? UiState.Success)?.data ?: emptyList()
    }
    val myBookmarksCount = remember(bookmarksState) {
        (bookmarksState as? UiState.Success)?.data?.size ?: 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. PREMIUM HERO PROFILE CARD (No "Message" button on own profile)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, QawmiGold.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Royal Emerald Banner with Avatar & Role
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(QawmiDarkGreen, QawmiDeepGreen, Color(0xFF0E684D))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IslamicStarEmblem(size = 16.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ব্যক্তিগত ও ইলমি প্রোফাইল",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QawmiGoldLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF00C853).copy(alpha = 0.2f),
                                modifier = Modifier.border(1.dp, Color(0xFF69F0AE), RoundedCornerShape(50))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "অনলাইন (সক্রিয়)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Profile Avatar with Camera Picker Overlay
                        Box(contentAlignment = Alignment.BottomEnd) {
                            UserAvatarImage(
                                photoUrl = photoUrl,
                                displayName = fullName.ifBlank { currentUserDisplayName },
                                size = 94.dp,
                                borderColor = QawmiGold,
                                borderWidth = 2.5.dp,
                                showOnlineIndicator = false,
                                modifier = Modifier
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("profile_avatar_box")
                            )
                            Surface(
                                shape = CircleShape,
                                color = QawmiGold,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("profile_change_photo_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "প্রোফাইল ছবি পরিবর্তন করুন",
                                        tint = QawmiDarkGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Display Name + Verified Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = fullName.ifBlank { currentUserDisplayName.ifBlank { "সম্মানিত দ্বীনি ভাই" } },
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (profile?.isVerified == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "যাচাইকৃত প্রোফাইল",
                                    tint = QawmiGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (designation.isNotBlank() || madrasaName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = listOf(designation, madrasaName).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = QawmiGoldLight,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Account Type & District Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (accountType == "muhtamim") {
                                        "মুহতামিম / মাদরাসা কর্তৃপক্ষ"
                                    } else {
                                        "খেদমতপ্রার্থী / মুদাররিস"
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (district.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = QawmiGold.copy(alpha = 0.25f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = QawmiGoldLight,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = district,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Photo Selection Bar (Gallery + Scholarly Avatar Presets)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QawmiMintSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "প্রোফাইল ছবি ও প্রতীক:",
                                style = MaterialTheme.typography.labelMedium,
                                color = QawmiTextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("pick_gallery_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = QawmiDeepGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "গ্যালারি থেকে ছবি নিন",
                                style = MaterialTheme.typography.labelSmall,
                                color = QawmiDeepGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfilePhotoHelper.presetAvatars.forEach { (presetKey, label) ->
                            val isSelected = photoUrl == presetKey
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    photoUrl = presetKey
                                    onUpdatePhotoImmediately(presetKey)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = QawmiDeepGreen,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("preset_avatar_$presetKey")
                            )
                        }
                    }
                }
            }
        }

        // 2. ACTIVITY & STATS METRICS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileMetricCard(
                count = myPublishedPosts.size,
                label = "প্রকাশিত পোস্ট",
                icon = Icons.Default.Article,
                modifier = Modifier.weight(1f),
                onClick = { selectedActivityTab = 0 }
            )
            ProfileMetricCard(
                count = mySentSubmissions.size,
                label = "প্রেরিত আবেদন",
                icon = Icons.AutoMirrored.Filled.Assignment,
                modifier = Modifier.weight(1f),
                onClick = { selectedActivityTab = 1 }
            )
            ProfileMetricCard(
                count = myReceivedSubmissions.size,
                label = "প্রাপ্ত আবেদন",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(MainDestination.ABEDON) }
            )
            ProfileMetricCard(
                count = myBookmarksCount,
                label = "সংরক্ষিত",
                icon = Icons.Default.Bookmark,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(MainDestination.BOOKMARKS) }
            )
        }

        // 3. CLEAN PERSONAL & ILMI IDENTITY PRESENTATION CARD
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ইলমি ও ব্যক্তিগত পরিচিতি",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { isEditExpanded = !isEditExpanded },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("toggle_edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = QawmiDeepGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditExpanded) "সংক্ষিপ্ত ভিউ" else "তথ্য সম্পাদনা",
                            style = MaterialTheme.typography.labelSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (bio.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QawmiMintSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = bio,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = QawmiTextDark
                        )
                    }
                }

                ProfileInfoRow(
                    icon = Icons.Default.School,
                    label = "ইলমি / শিক্ষাগত যোগ্যতা",
                    value = qualification.ifBlank { "উল্লেখ করা হয়নি" }
                )
                ProfileInfoRow(
                    icon = Icons.Default.WorkOutline,
                    label = "পদবী ও খেদমতের ক্ষেত্র",
                    value = designation.ifBlank { "উল্লেখ করা হয়নি" }
                )
                ProfileInfoRow(
                    icon = Icons.Default.WorkspacePremium,
                    label = "খেদমতের তাজরিবা (অভিজ্ঞতা)",
                    value = experience.ifBlank { "উল্লেখ করা হয়নি" }
                )
                ProfileInfoRow(
                    icon = Icons.Default.AccountBalance,
                    label = "বর্তমান / সংশ্লিষ্ট মাদরাসা",
                    value = madrasaName.ifBlank { "উল্লেখ করা হয়নি" }
                )
                ProfileInfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "নিজ জেলা ও পছন্দের কর্মস্থল",
                    value = buildString {
                        append(district.ifBlank { "জেলা উল্লেখ নেই" })
                        if (preferredDistricts.isNotBlank()) {
                            append(" • পছন্দের জেলা: ")
                            append(preferredDistricts)
                        }
                    }
                )
                ProfileInfoRow(
                    icon = Icons.Default.Phone,
                    label = "যোগাযোগের মোবাইল নম্বর",
                    value = contactInfo.ifBlank { "উল্লেখ করা হয়নি" }
                )
                ProfileInfoRow(
                    icon = Icons.Default.Email,
                    label = "সংযুক্ত গুগল ইমেইল",
                    value = currentUserEmail.ifBlank { "সংযুক্ত গুগল অ্যাকাউন্ট" }
                )
            }
        }

        // 4. EDIT PROFILE FORM CARD (Saves directly to Firebase Firestore)
        AnimatedVisibility(visible = isEditExpanded) {
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
                    Text(
                        text = "প্রোফাইল তথ্য সম্পাদনা ও হালনাগাদ",
                        style = MaterialTheme.typography.titleMedium,
                        color = QawmiTextDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "আপনার প্রোফাইল তথ্য ও ছবি Firebase-এ সংরক্ষিত থাকবে এবং অন্যান্য অনুমোদিত ব্যবহারকারীদের কাছে দৃশ্যমান হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = QawmiTextSecondary
                    )

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

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("পূর্ণ নাম *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("নিজ জেলা") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_district_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = contactInfo,
                            onValueChange = { contactInfo = it },
                            label = { Text("মোবাইল নম্বর") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_phone_input"),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = designation,
                        onValueChange = { designation = it },
                        label = { Text("পদবী / খেদমতের ক্ষেত্র") },
                        placeholder = { Text("যেমন: মুহাদ্দিস / সহকারী শিক্ষক / মুহতামিম") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_designation_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = qualification,
                        onValueChange = { qualification = it },
                        label = { Text("শিক্ষাগত / ইলমি যোগ্যতা") },
                        placeholder = { Text("যেমন: দাওরায়ে হাদীস (বেফাক), ইফতা") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_qualification_input"),
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
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("সংক্ষিপ্ত পরিচিতি ও দক্ষতার বিবরণ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("profile_bio_input")
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
                                madrasaName,
                                photoUrl
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
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("সংরক্ষণ হচ্ছে...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("প্রোফাইল আপডেট করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. RELEVANT POSTS, APPLICATIONS & ACTIVITIES SECTION
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
                Text(
                    text = "আমার পোস্ট, আবেদন ও খেদমত কার্যক্রম",
                    style = MaterialTheme.typography.titleMedium,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedActivityTab == 0,
                        onClick = { selectedActivityTab = 0 },
                        label = {
                            Text(
                                "প্রকাশিত পোস্ট (${IslamicDateAndPrayerHelper.toBanglaDigits(myPublishedPosts.size.toString())})"
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QawmiDeepGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("profile_tab_posts")
                    )
                    FilterChip(
                        selected = selectedActivityTab == 1,
                        onClick = { selectedActivityTab = 1 },
                        label = {
                            val totalApps = mySentSubmissions.size + myReceivedSubmissions.size
                            Text(
                                "আবেদন ও কার্যক্রম (${IslamicDateAndPrayerHelper.toBanglaDigits(totalApps.toString())})"
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QawmiDeepGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("profile_tab_applications")
                    )
                }

                if (selectedActivityTab == 0) {
                    if (myPublishedPosts.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = QawmiSoftBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "আপনি এখনো কোনো নিয়োগ বিজ্ঞপ্তি বা খেদমতের আবেদন পোস্ট করেননি।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = QawmiTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigate(MainDestination.PUBLISH_CIRCULAR) },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("বিজ্ঞপ্তি প্রকাশ", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { onNavigate(MainDestination.PUBLISH_APPLICATION) },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("আবেদন প্রকাশ", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    } else {
                        myPublishedPosts.forEach { post ->
                            val isCircular = post.postType == "JOB_CIRCULAR"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(12.dp))
                                    .clickable { onViewPostDetail(post) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = QawmiSoftBg)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (isCircular) QawmiDeepGreen else QawmiSeekerBlue
                                            ) {
                                                Text(
                                                    text = if (isCircular) "নিয়োগ বিজ্ঞপ্তি" else "খেদমত আবেদন",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            if (post.isBoosted) {
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = QawmiGold.copy(alpha = 0.25f)
                                                ) {
                                                    Text(
                                                        text = "Boosted",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = QawmiDarkGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = post.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = QawmiTextDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "পদ: ${post.designation} • জেলা: ${post.district}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = QawmiTextSecondary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteOwnPost(post.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "পোস্ট মুছুন",
                                            tint = Color(0xFFC62828)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (mySentSubmissions.isEmpty() && myReceivedSubmissions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = QawmiSoftBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "এখনো কোনো খেদমত আবেদন প্রেরিত বা প্রাপ্ত হয়নি।",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = QawmiTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        mySentSubmissions.take(5).forEach { sub ->
                            SubmissionCompactRow(
                                title = sub.postTitle,
                                subtitle = "প্রেরিত আবেদন • পদ: ${sub.postDesignation}",
                                status = sub.status,
                                onClick = { onNavigate(MainDestination.ABEDON) }
                            )
                        }
                        myReceivedSubmissions.take(5).forEach { sub ->
                            SubmissionCompactRow(
                                title = "${sub.applicantName} (${sub.postTitle})",
                                subtitle = "প্রাপ্ত আবেদন • পদ: ${sub.postDesignation}",
                                status = sub.status,
                                onClick = { onNavigate(MainDestination.ABEDON) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetricCard(
    count: Int,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, QawmiMintBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = QawmiDeepGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = IslamicDateAndPrayerHelper.toBanglaDigits(count.toString()),
                style = MaterialTheme.typography.titleMedium,
                color = QawmiDeepGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = QawmiTextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = QawmiMintSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = QawmiDeepGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = QawmiTextMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = QawmiTextDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SubmissionCompactRow(
    title: String,
    subtitle: String,
    status: String,
    onClick: () -> Unit
) {
    val statusLabel = when (status) {
        "SHORTLISTED" -> "মুযাকারার জন্য নির্বাচিত"
        "ACCEPTED" -> "কবুল / চূড়ান্ত"
        "REJECTED" -> "প্রত্যাখ্যাত"
        else -> "বিবেচনাধীন"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QawmiMintBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = QawmiSoftBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = QawmiTextDark,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = QawmiTextSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = QawmiMintSurface
            ) {
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = QawmiDeepGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Public Profile Inspection Dialog/Sheet for viewing another user's profile.
 * Strictly enforces:
 * - Shows "পয়গাম / মেসেজ পাঠান" (Message) button ONLY when viewing ANOTHER user's profile (targetUserId != currentUserId).
 * - Never shows the Message button on the user's own profile.
 */
@Composable
fun PublicUserProfileDialog(
    targetUserId: String,
    fallbackName: String,
    currentUserId: String,
    inspectedProfileState: UiState<UserProfile?>,
    userPublishedPosts: List<FeedPost>,
    onDismiss: () -> Unit,
    onSendMessageClick: (userId: String, userName: String) -> Unit,
    onPostClick: (FeedPost) -> Unit
) {
    val isOwnProfile = targetUserId == currentUserId
    val profile = (inspectedProfileState as? UiState.Success)?.data
    val displayName = profile?.fullName?.takeIf { it.isNotBlank() } ?: fallbackName.ifBlank { "সম্মানিত দ্বীনি ভাই" }
    val photoUrl = profile?.photoUrl.orEmpty()
    val isOnline = profile?.isOnline == true
    val lastSeenText = remember(profile?.isOnline, profile?.lastSeenAt) {
        if (profile?.isOnline == true) {
            "অনলাইন (এখন সক্রিয়)"
        } else if (profile?.lastSeenAt != null) {
            "সর্বশেষ সক্রিয়: ${IslamicDateAndPrayerHelper.formatRelativeTimeBangla(profile.lastSeenAt)}"
        } else {
            "অফলাইন"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .border(1.5.dp, QawmiGold, RoundedCornerShape(22.dp))
                .testTag("public_user_profile_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(QawmiDarkGreen, QawmiDeepGreen)
                            )
                        )
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .testTag("close_public_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserAvatarImage(
                            photoUrl = photoUrl,
                            displayName = displayName,
                            size = 84.dp,
                            borderColor = QawmiGold,
                            borderWidth = 2.5.dp,
                            showOnlineIndicator = true,
                            isOnline = isOnline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (profile?.isVerified == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "যাচাইকৃত",
                                    tint = QawmiGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastSeenText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOnline) Color(0xFF69F0AE) else QawmiGoldLight,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (profile?.accountType == "muhtamim") {
                                    "মুহতামিম / মাদরাসা কর্তৃপক্ষ"
                                } else {
                                    "খেদমতপ্রার্থী / মুদাররিস"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // MESSAGE OPTION ONLY ON OTHER USER'S PROFILE (Never on own profile)
                        if (!isOwnProfile && targetUserId.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onSendMessageClick(targetUserId, displayName) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QawmiGold,
                                    contentColor = QawmiDarkGreen
                                ),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("public_profile_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MailOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "পয়গাম / মেসেজ পাঠান",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Body Details
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!profile?.bio.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = QawmiMintSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = profile?.bio.orEmpty(),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = QawmiTextDark
                            )
                        }
                    }

                    ProfileInfoRow(
                        icon = Icons.Default.WorkOutline,
                        label = "পদবী / খেদমতের ক্ষেত্র",
                        value = profile?.designation?.ifBlank { "উল্লেখ করা হয়নি" } ?: "উল্লেখ করা হয়নি"
                    )
                    ProfileInfoRow(
                        icon = Icons.Default.School,
                        label = "শিক্ষাগত / ইলমি যোগ্যতা",
                        value = profile?.qualification?.ifBlank { "উল্লেখ করা হয়নি" } ?: "উল্লেখ করা হয়নি"
                    )
                    ProfileInfoRow(
                        icon = Icons.Default.WorkspacePremium,
                        label = "খেদমতের অভিজ্ঞতা",
                        value = profile?.experience?.ifBlank { "উল্লেখ করা হয়নি" } ?: "উল্লেখ করা হয়নি"
                    )
                    ProfileInfoRow(
                        icon = Icons.Default.AccountBalance,
                        label = "সংশ্লিষ্ট মাদরাসা",
                        value = profile?.madrasaName?.ifBlank { "উল্লেখ করা হয়নি" } ?: "উল্লেখ করা হয়নি"
                    )
                    ProfileInfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "জেলা ও পছন্দের কর্মস্থল",
                        value = buildString {
                            append(profile?.district?.ifBlank { "উল্লেখ নেই" } ?: "উল্লেখ নেই")
                            if (!profile?.preferredDistricts.isNullOrBlank()) {
                                append(" • পছন্দের জেলা: ${profile?.preferredDistricts}")
                            }
                        }
                    )
                    if (!profile?.contactInfo.isNullOrBlank()) {
                        ProfileInfoRow(
                            icon = Icons.Default.Phone,
                            label = "যোগাযোগ নম্বর",
                            value = profile?.contactInfo.orEmpty()
                        )
                    }

                    if (userPublishedPosts.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "প্রকাশিত খেদমত পোস্টসমূহ (${IslamicDateAndPrayerHelper.toBanglaDigits(userPublishedPosts.size.toString())})",
                            style = MaterialTheme.typography.titleSmall,
                            color = QawmiDeepGreen,
                            fontWeight = FontWeight.Bold
                        )
                        userPublishedPosts.take(4).forEach { post ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, QawmiMintBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        onDismiss()
                                        onPostClick(post)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = QawmiSoftBg)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = post.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = QawmiTextDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "পদ: ${post.designation} • জেলা: ${post.district}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = QawmiTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
