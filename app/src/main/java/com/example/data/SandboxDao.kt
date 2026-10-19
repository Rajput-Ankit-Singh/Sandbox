package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SandboxDao {
    // Profiles
    @Query("SELECT * FROM sandbox_profiles ORDER BY isDefault DESC, createdAt ASC")
    fun getAllProfiles(): Flow<List<SandboxProfileEntity>>

    @Query("SELECT * FROM sandbox_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: String): SandboxProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SandboxProfileEntity)

    @Query("DELETE FROM sandbox_profiles WHERE profileId = :profileId AND isDefault = 0")
    suspend fun deleteProfile(profileId: String)

    // Installed Apps
    @Query("SELECT * FROM installed_sandbox_apps WHERE profileId = :profileId ORDER BY installedAt DESC")
    fun getInstalledAppsForProfile(profileId: String): Flow<List<InstalledSandboxAppEntity>>

    @Query("SELECT * FROM installed_sandbox_apps ORDER BY installedAt DESC")
    fun getAllInstalledApps(): Flow<List<InstalledSandboxAppEntity>>

    @Query("SELECT * FROM installed_sandbox_apps WHERE profileId = :profileId AND packageName = :packageName LIMIT 1")
    suspend fun getInstalledApp(profileId: String, packageName: String): InstalledSandboxAppEntity?

    @Query("SELECT * FROM installed_sandbox_apps WHERE id = :id LIMIT 1")
    suspend fun getInstalledAppById(id: Long): InstalledSandboxAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: InstalledSandboxAppEntity): Long

    @Update
    suspend fun updateApp(app: InstalledSandboxAppEntity)

    @Query("DELETE FROM installed_sandbox_apps WHERE id = :id")
    suspend fun deleteAppById(id: Long)

    @Query("DELETE FROM installed_sandbox_apps WHERE profileId = :profileId")
    suspend fun deleteAllAppsInProfile(profileId: String)

    // Logs
    @Query("SELECT * FROM sandbox_logs WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT 200")
    fun getLogsForProfile(profileId: String): Flow<List<SandboxLogEntity>>

    @Query("SELECT * FROM sandbox_logs WHERE profileId = :profileId AND packageName = :packageName ORDER BY timestamp DESC LIMIT 200")
    fun getLogsForApp(profileId: String, packageName: String): Flow<List<SandboxLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SandboxLogEntity)

    @Query("DELETE FROM sandbox_logs WHERE profileId = :profileId AND packageName = :packageName")
    suspend fun clearLogsForApp(profileId: String, packageName: String)

    @Query("DELETE FROM sandbox_logs WHERE profileId = :profileId")
    suspend fun clearLogsForProfile(profileId: String)
}
