import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { IDatabaseVersion } from './database-version.model';

const initialState = {
  loading: false,
  errorMessage: null,
  databaseVersions: [] as ReadonlyArray<IDatabaseVersion>,
  databaseVersion: null as IDatabaseVersion | null,
  activeDatabaseVersions: [] as ReadonlyArray<IDatabaseVersion>,
};

export type DatabaseVersionManagementState = Readonly<typeof initialState>;

// Actions
export const getDatabaseVersions = createAsyncThunk('databaseVersionManagement/fetch_all', async () => {
  const requestUrl = 'api/database-vendor-versions';
  return axios.get<IDatabaseVersion[]>(requestUrl);
});

export const getDatabaseVersion = createAsyncThunk('databaseVersionManagement/fetch_one', async (id: string | number) => {
  const requestUrl = `api/database-vendor-versions/${id}`;
  return axios.get<IDatabaseVersion>(requestUrl);
});

export const getDatabaseVersionsByDatabaseId = createAsyncThunk(
  'databaseVersionManagement/fetch_by_database',
  async (databaseId: string | number) => {
    const requestUrl = `api/database-vendor-versions?databaseId=${databaseId}`;
    return axios.get<IDatabaseVersion[]>(requestUrl);
  },
);

export const createDatabaseVersion = createAsyncThunk('databaseVersionManagement/create', async (version: IDatabaseVersion) => {
  const result = await axios.post<IDatabaseVersion>('api/database-vendor-versions', version);
  return result;
});

export const updateDatabaseVersion = createAsyncThunk('databaseVersionManagement/update', async (version: IDatabaseVersion) => {
  const result = await axios.put<IDatabaseVersion>(`api/database-vendor-versions/${version.id}`, version);
  return result;
});

export const deleteDatabaseVersion = createAsyncThunk('databaseVersionManagement/delete', async (id: string | number) => {
  const requestUrl = `api/database-vendor-versions/${id}`;
  await axios.delete(requestUrl);
  return id;
});

export const DatabaseVersionManagementSlice = createSlice({
  name: 'databaseVersionManagement',
  initialState: initialState as DatabaseVersionManagementState,
  reducers: {
    reset() {
      return initialState;
    },
  },
  extraReducers(builder) {
    builder
      .addCase(getDatabaseVersions.pending, state => {
        state.loading = true;
      })
      .addCase(getDatabaseVersions.fulfilled, (state, action) => {
        state.loading = false;
        const versions = action.payload.data;
        state.databaseVersions = versions;
        state.activeDatabaseVersions = versions.filter(v => v.active !== false);
      })
      .addCase(getDatabaseVersions.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getDatabaseVersion.pending, state => {
        state.loading = true;
      })
      .addCase(getDatabaseVersion.fulfilled, (state, action) => {
        state.loading = false;
        state.databaseVersion = action.payload.data;
      })
      .addCase(getDatabaseVersion.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getDatabaseVersionsByDatabaseId.fulfilled, (state, action) => {
        state.loading = false;
        const versions = action.payload.data;
        state.databaseVersions = versions;
        state.activeDatabaseVersions = versions.filter(v => v.active !== false);
      })
      .addCase(createDatabaseVersion.fulfilled, (state, action) => {
        state.loading = false;
        const version = action.payload.data;
        state.databaseVersions = [...state.databaseVersions, version];
        if (version.active !== false) {
          state.activeDatabaseVersions = [...state.activeDatabaseVersions, version];
        }
      })
      .addCase(updateDatabaseVersion.fulfilled, (state, action) => {
        state.loading = false;
        const version = action.payload.data;
        const index = state.databaseVersions.findIndex(v => v.id === version.id);
        if (index >= 0) {
          state.databaseVersions[index] = version;
        }
        if (state.databaseVersion?.id === version.id) {
          state.databaseVersion = version;
        }
        if (version.active !== false) {
          const activeIndex = state.activeDatabaseVersions.findIndex(v => v.id === version.id);
          if (activeIndex >= 0) {
            state.activeDatabaseVersions[activeIndex] = version;
          } else {
            state.activeDatabaseVersions = [...state.activeDatabaseVersions, version];
          }
        } else {
          state.activeDatabaseVersions = state.activeDatabaseVersions.filter(v => v.id !== version.id);
        }
      })
      .addCase(deleteDatabaseVersion.fulfilled, (state, action) => {
        state.loading = false;
        const id = action.payload;
        state.databaseVersions = state.databaseVersions.filter(v => v.id !== id);
        state.activeDatabaseVersions = state.activeDatabaseVersions.filter(v => v.id !== id);
        if (state.databaseVersion?.id === id) {
          state.databaseVersion = null;
        }
      });
  },
});

export const { reset } = DatabaseVersionManagementSlice.actions;

export default DatabaseVersionManagementSlice.reducer;
