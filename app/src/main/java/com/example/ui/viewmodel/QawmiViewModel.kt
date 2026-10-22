package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRegistryEntry
import com.example.data.model.AppNotification
import com.example.data.model.BookmarkItem
import com.example.data.model.DirectMessage
import com.example.data.model.FeedPost
import com.example.data.model.KhedmotSubmission
import com.example.data.model.MadrasaProfile
import com.example.data.model.TelegramNotificationLog
import com.example.data.model.UserProfile
import com.example.data.repository.QawmiRepository
import com.example.ui.util.AdminSecurityHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainDestination {
    HOME,
    FEED,
    KHEDMOT,
    ABEDON,
    MADRASA,
    PROFILE,
    BOOKMARKS,
    VERIFIED_MADRASAS,
    PUBLISH_CIRCULAR,
    PUBLISH_APPLICATION,
    REGISTER_MADRASA,
    MESSAGES,
    NOTIFICATIONS,
    ADMIN_PANEL,
    PRAYER_SCHEDULE
}

enum class FeedFilterTab {
    ALL,            // সব পোস্ট
    JOB_CIRCULAR,   // নিয়োগ বিজ্ঞপ্তি
    APPLICATIONS,   // আবেদন
    DISTRICT        // জেলা
}

@OptIn(ExperimentalCoroutinesApi::class)
class QawmiViewModel(
    private val repository: QawmiRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String
) : ViewModel() {

    private val _currentDestination = MutableStateFlow(MainDestination.HOME)
    val currentDestination: StateFlow<MainDestination> = _currentDestination.asStateFlow()

    private val _navigationStack = ArrayDeque<MainDestination>()

    private val _selectedFeedFilter = MutableStateFlow(FeedFilterTab.ALL)
    val selectedFeedFilter: StateFlow<FeedFilterTab> = _selectedFeedFilter.asStateFlow()

    private val _selectedDistrictFilter = MutableStateFlow("সকল জেলা")
    val selectedDistrictFilter: StateFlow<String> = _selectedDistrictFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _feedbackBanner = MutableStateFlow<String?>(null)
    val feedbackBanner: StateFlow<String?> = _feedbackBanner.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // Active dialog / detail targets
    private val _selectedPostForApply = MutableStateFlow<FeedPost?>(null)
    val selectedPostForApply: StateFlow<FeedPost?> = _selectedPostForApply.asStateFlow()

    private val _selectedPostForDetail = MutableStateFlow<FeedPost?>(null)
    val selectedPostForDetail: StateFlow<FeedPost?> = _selectedPostForDetail.asStateFlow()

    private val _messageTargetUser = MutableStateFlow<Pair<String, String>?>(null) // (uid, name)
    val messageTargetUser: StateFlow<Pair<String, String>?> = _messageTargetUser.asStateFlow()

    // Public profile inspection target (uid to fallbackName)
    private val _inspectedUserTarget = MutableStateFlow<Pair<String, String>?>(null)
    val inspectedUserTarget: StateFlow<Pair<String, String>?> = _inspectedUserTarget.asStateFlow()

    // Server-side Admin Registry & Token Claim state
    private val _serverTokenVerifiedAdmin = MutableStateFlow(
        AdminSecurityHelper.isAuthorizedAdminEmail(currentUserEmail)
    )

    val adminRegistryEntryState: StateFlow<AdminRegistryEntry?> = repository.observeAdminRegistryEntry(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val isAdminState: StateFlow<Boolean> = combine(
        _serverTokenVerifiedAdmin,
        adminRegistryEntryState
    ) { tokenVerified, registryEntry ->
        AdminSecurityHelper.isFirebaseAuthUserAdmin(
            authEmail = currentUserEmail,
            hasCustomAdminClaim = tokenVerified,
            hasServerRegistryEntry = registryEntry?.verifiedByAuthToken == true
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AdminSecurityHelper.isAuthorizedAdminEmail(currentUserEmail)
    )

    val isAdminUser: Boolean
        get() = isAdminState.value

    // Real-time Firestore StateFlows (Two-tier error handling)
    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(UiState.Error(e.localizedMessage ?: "প্রোফাইল লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val inspectedUserProfileState: StateFlow<UiState<UserProfile?>> = _inspectedUserTarget
        .flatMapLatest { target ->
            if (target == null || target.first.isBlank()) {
                flowOf(UiState.Success(null))
            } else {
                repository.observeUserProfile(target.first)
                    .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing inspected user profile", e)
                        emit(UiState.Error(e.localizedMessage ?: "ব্যবহারকারীর প্রোফাইল লোড করা যায়নি"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val feedPostsState: StateFlow<UiState<List<FeedPost>>> = repository.observePublicFeedPosts()
        .map<List<FeedPost>, UiState<List<FeedPost>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing feed posts", e)
            emit(UiState.Error(e.localizedMessage ?: "ফিড লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val bookmarksState: StateFlow<UiState<List<BookmarkItem>>> = repository.observeBookmarks(currentUserId)
        .map<List<BookmarkItem>, UiState<List<BookmarkItem>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing bookmarks", e)
            emit(UiState.Error(e.localizedMessage ?: "পছন্দের খেদমত লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val madrasasState: StateFlow<UiState<List<MadrasaProfile>>> = repository.observePublicMadrasas()
        .map<List<MadrasaProfile>, UiState<List<MadrasaProfile>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing madrasas", e)
            emit(UiState.Error(e.localizedMessage ?: "মাদরাসার তালিকা লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val mySentSubmissionsState: StateFlow<UiState<List<KhedmotSubmission>>> = repository.observeMySentSubmissions(currentUserId)
        .map<List<KhedmotSubmission>, UiState<List<KhedmotSubmission>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing sent submissions", e)
            emit(UiState.Error(e.localizedMessage ?: "আবেদন তালিকা লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val receivedSubmissionsState: StateFlow<UiState<List<KhedmotSubmission>>> = repository.observeReceivedSubmissions(currentUserId)
        .map<List<KhedmotSubmission>, UiState<List<KhedmotSubmission>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing received submissions", e)
            emit(UiState.Error(e.localizedMessage ?: "প্রাপ্ত আবেদন লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // Optimistic local read message IDs so unread badge decrements/clears immediately while Firestore batch commits
    private val _locallyReadMessageIds = MutableStateFlow<Set<String>>(emptySet())

    val messagesState: StateFlow<UiState<List<DirectMessage>>> = combine(
        repository.observeUserMessages(currentUserId),
        _locallyReadMessageIds
    ) { list, locallyReadIds ->
        val deduplicated = list
            .filter { it.id.isNotBlank() }
            .filterNot { msg ->
                (msg.senderId == currentUserId && msg.deletedForSender) ||
                    (msg.receiverId == currentUserId && msg.deletedForReceiver)
            }
            .distinctBy { it.id }
            .map { msg ->
                if (!msg.isRead && locallyReadIds.contains(msg.id)) {
                    msg.copy(isRead = true)
                } else {
                    msg
                }
            }
        UiState.Success(deduplicated) as UiState<List<DirectMessage>>
    }
        .catch { e ->
            Log.w(TAG, "Error observing messages", e)
            emit(UiState.Error(e.localizedMessage ?: "মেসেজ লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    // Real-time profiles of all conversation partners in the inbox (for Online/Offline, Last Seen & Avatar)
    val chatPartnerProfilesState: StateFlow<Map<String, UserProfile>> = combine(
        messagesState,
        _messageTargetUser
    ) { msgState, targetUser ->
        val messages = (msgState as? UiState.Success)?.data ?: emptyList()
        val partnerIds = messages.map { msg ->
            if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
        }.toMutableSet()
        targetUser?.first?.takeIf { it.isNotBlank() }?.let { partnerIds.add(it) }
        partnerIds.filter { it.isNotBlank() && it != currentUserId }
    }.flatMapLatest { validIds ->
        if (validIds.isEmpty()) {
            flowOf(emptyMap())
        } else {
            val flows = validIds.map { partnerUid ->
                repository.observeUserProfile(partnerUid)
                    .map { profile -> partnerUid to profile }
                    .catch { emit(partnerUid to null) }
            }
            combine(flows) { pairs ->
                pairs.mapNotNull { (uid, profile) ->
                    if (profile != null) uid to profile else null
                }.toMap()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val activeChatPartnerProfileState: StateFlow<UserProfile?> = _messageTargetUser
        .flatMapLatest { target ->
            val uid = target?.first.orEmpty()
            if (uid.isBlank() || uid == currentUserId) {
                flowOf(null)
            } else {
                repository.observeUserProfile(uid)
                    .catch { emit(null) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val notificationsState: StateFlow<UiState<List<AppNotification>>> = repository.observeNotifications(currentUserId)
        .map<List<AppNotification>, UiState<List<AppNotification>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing notifications", e)
            emit(UiState.Error(e.localizedMessage ?: "নোটিফিকেশন লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    // Real-time Telegram notifications queue (strictly observed ONLY when verified Admin is logged in)
    val telegramNotificationsState: StateFlow<UiState<List<TelegramNotificationLog>>> = isAdminState
        .flatMapLatest { isAdmin ->
            if (!isAdmin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeTelegramNotifications()
                    .map<List<TelegramNotificationLog>, UiState<List<TelegramNotificationLog>>> { UiState.Success(it) }
                    .catch { e ->
                        Log.w(TAG, "Error observing Telegram notifications", e)
                        emit(UiState.Error(e.localizedMessage ?: "টেলিগ্রাম নোটিফিকেশন লগ লোড করা যায়নি"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        setUserOnlineStatus(true)
        verifyAndSyncAdminIdentity()
    }

    private fun verifyAndSyncAdminIdentity() {
        viewModelScope.launch {
            val result = repository.ensureVerifiedAdminRegistryDocument()
            result.onSuccess { isVerifiedAdmin ->
                _serverTokenVerifiedAdmin.value = isVerifiedAdmin
            }
        }
    }

    fun setUserOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateUserPresence(
                isOnline = isOnline,
                defaultFullName = resolveActorDisplayName("সম্মানিত দ্বীনি ভাই")
            )
        }
    }

    private fun resolveActorDisplayName(fallback: String = "কওমি কর্মসংস্থান"): String {
        return (userProfileState.value as? UiState.Success)?.data?.fullName
            ?.takeIf { it.isNotBlank() }
            ?: currentUserDisplayName.ifBlank { fallback }
    }

    private fun resolveActorPhotoUrl(): String {
        return (userProfileState.value as? UiState.Success)?.data?.photoUrl.orEmpty()
    }

    fun navigateTo(destination: MainDestination) {
        if (destination == MainDestination.ADMIN_PANEL && !isAdminUser) {
            _errorBanner.value = "অনুমোদনহীন প্রবেশাধিকার: শুধুমাত্র যাচাইকৃত অ্যাডমিন অ্যাকাউন্ট দিয়ে অ্যাডমিন প্যানেল ব্যবহার করা যাবে।"
            return
        }
        if (_currentDestination.value != destination) {
            _navigationStack.addLast(_currentDestination.value)
            _currentDestination.value = destination
        }
    }

    fun navigateBottomTab(destination: MainDestination) {
        _navigationStack.clear()
        _currentDestination.value = destination
    }

    fun navigateBack(): Boolean {
        return if (_navigationStack.isNotEmpty()) {
            _currentDestination.value = _navigationStack.removeLast()
            true
        } else if (_currentDestination.value != MainDestination.HOME) {
            _currentDestination.value = MainDestination.HOME
            true
        } else {
            false
        }
    }

    fun setFeedFilter(tab: FeedFilterTab) {
        _selectedFeedFilter.value = tab
    }

    fun setDistrictFilter(district: String) {
        _selectedDistrictFilter.value = district
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openFeedWithFilter(tab: FeedFilterTab) {
        _selectedFeedFilter.value = tab
        navigateTo(MainDestination.FEED)
    }

    fun openApplyDialog(post: FeedPost?) {
        _selectedPostForApply.value = post
    }

    fun openPostDetailDialog(post: FeedPost?) {
        _selectedPostForDetail.value = post
    }

    fun openUserProfileInspection(userId: String, fallbackName: String) {
        if (userId.isBlank()) return
        if (userId == currentUserId) {
            _inspectedUserTarget.value = null
            navigateTo(MainDestination.PROFILE)
        } else {
            _inspectedUserTarget.value = userId to fallbackName
        }
    }

    fun closeUserProfileInspection() {
        _inspectedUserTarget.value = null
    }

    fun openDirectMessageTo(userId: String, userName: String) {
        if (userId.isBlank() || userId == currentUserId) return
        _inspectedUserTarget.value = null
        _messageTargetUser.value = userId to userName
        navigateTo(MainDestination.MESSAGES)
        markConversationRead(userId)
    }

    fun clearMessageTarget() {
        val previousTarget = _messageTargetUser.value?.first.orEmpty()
        _messageTargetUser.value = null
        if (previousTarget.isNotBlank()) {
            setTypingStatus(previousTarget, false)
        }
    }

    fun setTypingStatus(targetUserId: String, isTyping: Boolean) {
        viewModelScope.launch {
            repository.updateTypingStatus(
                targetUserId = targetUserId,
                isTyping = isTyping,
                defaultFullName = resolveActorDisplayName("সম্মানিত দ্বীনি ভাই")
            )
        }
    }

    fun clearBanners() {
        _feedbackBanner.value = null
        _errorBanner.value = null
    }

    fun saveProfile(
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
        photoUrl: String = "",
        onComplete: () -> Unit = {}
    ) {
        if (fullName.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে আপনার পূর্ণ নাম উল্লেখ করুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val existingPhoto = (userProfileState.value as? UiState.Success)?.data?.photoUrl.orEmpty()
            val profile = UserProfile(
                userId = currentUserId,
                fullName = fullName.trim(),
                accountType = accountType,
                district = district.trim(),
                designation = designation.trim(),
                qualification = qualification.trim(),
                experience = experience.trim(),
                preferredDistricts = preferredDistricts.trim(),
                bio = bio.trim(),
                contactInfo = contactInfo.trim(),
                madrasaName = madrasaName.trim(),
                photoUrl = photoUrl.ifBlank { existingPhoto },
                isOnline = true
            )
            val result = repository.saveUserProfile(profile)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার প্রোফাইল তথ্য ও ছবি সংরক্ষিত হয়েছে।"
                onComplete()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "প্রোফাইল সংরক্ষণে ত্রুটি হয়েছে।"
            }
        }
    }

    fun updateProfilePhotoImmediately(photoUrl: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val fallbackName = resolveActorDisplayName("আবদুল্লাহ")
            val result = repository.updateProfilePhoto(photoUrl, fallbackName)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার প্রোফাইল ছবি সফলভাবে সংরক্ষিত হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "প্রোফাইল ছবি সংরক্ষণে ত্রুটি হয়েছে।"
            }
        }
    }

    fun publishFeedPost(
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
        contactNumber: String,
        onSuccessNavigate: () -> Unit = {}
    ) {
        if (title.isBlank() || district.isBlank() || designation.isBlank() || qualification.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে শিরোনাম, জেলা, পদ এবং শিক্ষাগত যোগ্যতা পূরণ করুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val result = repository.createFeedPost(
                authorName = authorName.ifBlank { resolveActorDisplayName(title) },
                postType = postType,
                title = title,
                district = district,
                upazilaOrLocation = upazilaOrLocation.ifBlank { district },
                designation = designation,
                qualification = qualification,
                salaryOrExperience = salaryOrExperience.ifBlank { "আলোচনা সাপেক্ষ" },
                deadlineOrPreferredDistricts = deadlineOrPreferredDistricts.ifBlank { "চলমান" },
                description = description,
                contactNumber = contactNumber,
                authorPhotoUrl = resolveActorPhotoUrl()
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = if (postType == "JOB_CIRCULAR") {
                    "আলহামদুলিল্লাহ, খেদমতের নিয়োগ বিজ্ঞপ্তিটি ফিডে প্রকাশিত হয়েছে।"
                } else {
                    "আলহামদুলিল্লাহ, আপনার খেদমতের আবেদনটি ফিডে প্রকাশিত হয়েছে।"
                }
                onSuccessNavigate()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "প্রকাশ করতে সমস্যা হয়েছে।"
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            clearBanners()
            val result = repository.deleteFeedPost(postId)
            result.onSuccess {
                _feedbackBanner.value = "পোস্টটি মুছে ফেলা হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "মুছে ফেলতে সমস্যা হয়েছে।"
            }
        }
    }

    fun toggleBookmark(post: FeedPost, isCurrentlyBookmarked: Boolean) {
        viewModelScope.launch {
            clearBanners()
            val result = repository.toggleBookmark(post, isCurrentlyBookmarked)
            result.onSuccess { added ->
                _feedbackBanner.value = if (added) {
                    "পছন্দের খেদমত তালিকায় সংরক্ষিত হয়েছে।"
                } else {
                    "পছন্দের খেদমত তালিকা থেকে সরানো হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "সংরক্ষণ করা যায়নি।"
            }
        }
    }

    fun registerMadrasa(
        name: String,
        district: String,
        address: String,
        establishedYear: String,
        highestMarhala: String,
        facilities: String,
        studentCount: String,
        contactPhone: String,
        onSuccessNavigate: () -> Unit = {}
    ) {
        if (name.isBlank() || district.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে মাদরাসার নাম ও জেলা উল্লেখ করুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val result = repository.createMadrasa(
                name = name,
                district = district,
                address = address,
                establishedYear = establishedYear,
                highestMarhala = highestMarhala,
                facilities = facilities,
                studentCount = studentCount,
                contactPhone = contactPhone
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, মাদরাসার তথ্য ডিরেক্টরিতে যুক্ত হয়েছে।"
                onSuccessNavigate()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "মাদরাসা যুক্ত করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun submitKhedmotApplication(
        post: FeedPost,
        applicantName: String,
        applicantQualification: String,
        applicantExperience: String,
        applicantPhone: String,
        coverNote: String,
        onComplete: () -> Unit = {}
    ) {
        if (applicantName.isBlank() || applicantPhone.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে আপনার নাম ও যোগাযোগের নম্বর উল্লেখ করুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val result = repository.submitApplicationToCircular(
                post = post,
                applicantName = applicantName,
                applicantQualification = applicantQualification,
                applicantExperience = applicantExperience,
                applicantPhone = applicantPhone,
                coverNote = coverNote
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার খেদমতের আবেদন সংশ্লিষ্ট মাদরাসায় প্রেরিত হয়েছে।"
                _selectedPostForApply.value = null
                onComplete()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "আবেদন প্রেরণে ত্রুটি হয়েছে।"
            }
        }
    }

    fun updateReceivedSubmissionStatus(submission: KhedmotSubmission, newStatus: String) {
        viewModelScope.launch {
            clearBanners()
            val actorName = resolveActorDisplayName("মাদরাসা কর্তৃপক্ষ")
            val result = repository.updateSubmissionStatus(submission, newStatus, actorName)
            result.onSuccess {
                _feedbackBanner.value = "আবেদনের অবস্থা হালনাগাদ করা হয়েছে এবং প্রার্থীকে নোটিফিকেশন পাঠানো হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "অবস্থা পরিবর্তনে ত্রুটি হয়েছে।"
            }
        }
    }

    fun sendMessage(
        receiverId: String,
        receiverName: String,
        subject: String,
        body: String,
        onSent: () -> Unit = {}
    ) {
        if (receiverId.isBlank() || receiverId == currentUserId) {
            _errorBanner.value = "সঠিক প্রাপক নির্বাচন করুন।"
            return
        }
        if (body.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে মেসেজের মূল বক্তব্য লিখুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val senderName = resolveActorDisplayName("দ্বীনি ভাই")
            val senderPhoto = resolveActorPhotoUrl()
            val result = repository.sendDirectMessage(
                senderName = senderName,
                receiverId = receiverId,
                receiverName = receiverName,
                subject = subject.ifBlank { "খেদমত সংক্রান্ত যোগাযোগ" },
                body = body,
                senderPhotoUrl = senderPhoto
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার পয়গাম প্রাপকের কাছে প্রেরিত হয়েছে।"
                onSent()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "মেসেজ পাঠাতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun markMessageRead(messageId: String) {
        if (messageId.isBlank()) return
        _locallyReadMessageIds.value = _locallyReadMessageIds.value + messageId
        viewModelScope.launch {
            repository.markMessageAsRead(messageId)
        }
    }

    fun markConversationRead(otherUserId: String) {
        if (otherUserId.isBlank() || otherUserId == currentUserId) return
        val currentMessages = (messagesState.value as? UiState.Success)?.data ?: emptyList()
        val matchingUnreadIds = currentMessages
            .filter { it.senderId == otherUserId && it.receiverId == currentUserId && !it.isRead && it.id.isNotBlank() }
            .map { it.id }
            .toSet()
        if (matchingUnreadIds.isNotEmpty()) {
            _locallyReadMessageIds.value = _locallyReadMessageIds.value + matchingUnreadIds
        }
        viewModelScope.launch {
            repository.markConversationMessagesAsRead(otherUserId, currentMessages)
        }
    }

    fun reactToMessage(message: DirectMessage, reactionCode: String) {
        viewModelScope.launch {
            val result = repository.reactToMessage(message, reactionCode)
            result.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "অভিবাদন যুক্ত করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun editMessage(message: DirectMessage, newBody: String) {
        val trimmed = newBody.trim()
        if (trimmed.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে পয়গামের কথা লিখুন।"
            return
        }
        viewModelScope.launch {
            val result = repository.editDirectMessage(message, trimmed)
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার পয়গামটি সম্পাদিত হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পয়গাম সম্পাদনা করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun deleteMessageForMe(message: DirectMessage) {
        viewModelScope.launch {
            val result = repository.deleteMessageForMe(message)
            result.onSuccess {
                _feedbackBanner.value = "পয়গামটি আপনার কথোপকথন থেকে সরিয়ে দেওয়া হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পয়গাম মুছতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun deleteMessageForEveryone(message: DirectMessage) {
        viewModelScope.launch {
            val result = repository.deleteMessageForEveryone(message)
            result.onSuccess {
                _feedbackBanner.value = "পয়গামটি উভয়ের কথোপকথন থেকে প্রত্যাহার করা হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পয়গাম প্রত্যাহার করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun toggleBlockUser(targetUserId: String, block: Boolean) {
        if (targetUserId.isBlank() || targetUserId == currentUserId) return
        viewModelScope.launch {
            val defaultName = resolveActorDisplayName("সম্মানিত সদস্য")
            val result = repository.setBlockedUserStatus(targetUserId, block, defaultName)
            result.onSuccess {
                _feedbackBanner.value = if (block) {
                    "এই ব্যবহারকারীর সাথে পয়গাম আদান-প্রদান বিরত (ব্লক) রাখা হয়েছে।"
                } else {
                    "আলহামদুলিল্লাহ, এই ব্যবহারকারীর সাথে পয়গাম আদান-প্রদান পুনরায় চালু হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "ব্লক স্ট্যাটাস পরিবর্তন করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun saveConversationTheme(partnerUserId: String, themeSpec: String) {
        if (partnerUserId.isBlank()) return
        viewModelScope.launch {
            val defaultName = resolveActorDisplayName("সম্মানিত সদস্য")
            val result = repository.updateConversationTheme(partnerUserId, themeSpec, defaultName)
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, কথোপকথনের আবহ ও থিম সংরক্ষিত হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "থিম সংরক্ষণ করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun markNotificationRead(notification: AppNotification) {
        if (notification.isRead) return
        viewModelScope.launch {
            repository.markNotificationAsRead(notification)
        }
    }

    fun markAllNotificationsRead() {
        val currentList = (notificationsState.value as? UiState.Success)?.data ?: return
        val unreadList = currentList.filter { !it.isRead }
        if (unreadList.isEmpty()) return
        viewModelScope.launch {
            val result = repository.markAllNotificationsAsRead(unreadList)
            result.onSuccess {
                _feedbackBanner.value = "সকল নোটিফিকেশন পঠিত হিসেবে চিহ্নিত করা হয়েছে।"
            }
        }
    }

    fun openNotificationTarget(notification: AppNotification) {
        markNotificationRead(notification)
        when (notification.type) {
            "MESSAGE" -> {
                if (notification.senderId.isNotBlank() && notification.senderId != currentUserId) {
                    openDirectMessageTo(notification.senderId, notification.senderName)
                } else {
                    navigateTo(MainDestination.MESSAGES)
                }
            }
            "APPLICATION" -> navigateTo(MainDestination.ABEDON)
            "VERIFICATION" -> navigateTo(MainDestination.VERIFIED_MADRASAS)
            "NEW_CIRCULAR", "BOOST" -> {
                val matchingPost = (feedPostsState.value as? UiState.Success)?.data
                    ?.find { it.id == notification.referenceId }
                openFeedWithFilter(FeedFilterTab.JOB_CIRCULAR)
                if (matchingPost != null) {
                    openPostDetailDialog(matchingPost)
                }
            }
            "NEW_APPLICATION" -> {
                val matchingPost = (feedPostsState.value as? UiState.Success)?.data
                    ?.find { it.id == notification.referenceId }
                openFeedWithFilter(FeedFilterTab.APPLICATIONS)
                if (matchingPost != null) {
                    openPostDetailDialog(matchingPost)
                }
            }
            "ANNOUNCEMENT" -> {
                // Remains on Notifications after marking read
            }
        }
    }

    // --- ADMIN PANEL & SECURITY ACTIONS (Strictly gated by verified Admin Identity) ---

    fun adminVerifyMadrasa(madrasa: MadrasaProfile, verified: Boolean) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন এই কার্যক্রম পরিচালনা করতে পারবেন।"
            return
        }
        viewModelScope.launch {
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.adminToggleMadrasaVerification(madrasa, verified, adminName)
            result.onSuccess {
                _feedbackBanner.value = if (verified) {
                    "মাদরাসাটি 'যাচাইকৃত মাদরাসা' হিসেবে অনুমোদিত হয়েছে এবং টেলিগ্রাম ইত্তেলা লগ করা হয়েছে।"
                } else {
                    "মাদরাসার যাচাইকরণ স্ট্যাটাস পরিবর্তন করা হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "অ্যাডমিন অনুমোদন সম্পন্ন হয়নি।"
            }
        }
    }

    fun adminVerifyPost(post: FeedPost, isVerified: Boolean, approvalStatus: String) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন এই কার্যক্রম পরিচালনা করতে পারবেন।"
            return
        }
        viewModelScope.launch {
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.adminUpdatePostVerification(post, isVerified, approvalStatus, adminName)
            result.onSuccess {
                _feedbackBanner.value = "পোস্টের অনুমোদন অবস্থা হালনাগাদ হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পোস্ট হালনাগাদ ব্যর্থ হয়েছে।"
            }
        }
    }

    fun adminToggleBoostPost(post: FeedPost, isBoosted: Boolean) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন বিজ্ঞপ্তি Boost করতে পারবেন।"
            return
        }
        viewModelScope.launch {
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.adminTogglePostBoost(post, isBoosted, adminName)
            result.onSuccess {
                _feedbackBanner.value = if (isBoosted) {
                    "আলহামদুলিল্লাহ, বিজ্ঞপ্তিটি Boosted হিসেবে ফিডের উপরে স্থাপন করা হয়েছে।"
                } else {
                    "বিজ্ঞপ্তিটির Boosted অবস্থা সরানো হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "Boost অবস্থা পরিবর্তন ব্যর্থ হয়েছে।"
            }
        }
    }

    fun publishAdminAnnouncement(
        title: String,
        message: String,
        onSent: () -> Unit = {}
    ) {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন ঘোষণা প্রকাশ করতে পারবেন।"
            return
        }
        if (title.isBlank() || message.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে ঘোষণার শিরোনাম ও বিস্তারিত বার্তা লিখুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.sendAdminAnnouncement(adminName, title, message)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, অ্যাডমিন ঘোষণা সকল ব্যবহারকারীর কাছে ও টেলিগ্রাম লগে প্রেরিত হয়েছে।"
                onSent()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "ঘোষণা প্রেরণ করা যায়নি।"
            }
        }
    }

    fun requestAdminPasswordReset(email: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val result = repository.sendAdminPasswordResetLink(email)
            _isSubmitting.value = false
            result.onSuccess { sentEmail ->
                _feedbackBanner.value = "আলহামদুলিল্লাহ, অনুমোদিত অ্যাডমিন ইমেইল ($sentEmail)-এ Firebase Authentication থেকে নিরাপদ Password Reset Link পাঠানো হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পাসওয়ার্ড রিসেট লিংক পাঠানো যায়নি।"
            }
        }
    }

    fun sendTestTelegramNotification(customMessage: String = "") {
        if (!isAdminUser) {
            _errorBanner.value = "শুধুমাত্র অনুমোদিত অ্যাডমিন টেলিগ্রাম নোটিফিকেশন পরীক্ষা করতে পারবেন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val adminName = resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)")
            val result = repository.sendAdminTestTelegramNotification(adminName, customMessage)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, টেলিগ্রাম চ্যানেল (${com.example.ui.util.TelegramNotificationHelper.DEFAULT_TELEGRAM_CHANNEL}) ও অডিট লগে ইত্তেলা সফলভাবে সংরক্ষিত হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "টেলিগ্রাম নোটিফিকেশন পাঠাতে সমস্যা হয়েছে।"
            }
        }
    }

    suspend fun recordAdminLogoutBeforeSignOut() {
        if (!isAdminUser) return
        val adminName = currentUserEmail.ifBlank { resolveActorDisplayName("নাজিমে আলা (অ্যাডমিন)") }
        repository.recordAdminLogoutActivity(adminName)
    }

    private companion object {
        const val TAG = "QawmiViewModel"
    }
}
