import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { IDatabaseVendorVersion } from './database-vendor-version.model';
import { serializeAxiosError } from 'app/shared/reducers/reducer.utils';

const initialState = {
  loading: false,
  errorMessage: null,
  versions: [] as ReadonlyArray<IDatabaseVendorVersion>,
  recentVersions: [] as ReadonlyArray<IDatabaseVendorVersion>,
};

export type DatabaseVendorVersionState = Readonly<typeof initialState>;

// Actions

export const getVendorVersions = createAsyncThunk(
  'databaseVendorVersion/fetch_versions',
  async (vendorId: number) => {
    const requestUrl = `api/database-vendor-versions?vendorId=${vendorId}`;
    return axios.get<IDatabaseVendorVersion[]>(requestUrl);
  },
  { serializeError: serializeAxiosError },
);

export const getRecentVendorVersions = createAsyncThunk(
  'databaseVendorVersion/fetch_recent_versions',
  async ({ vendorId, years = 3 }: { vendorId: number; years?: number }) => {
    const requestUrl = `api/database-vendor-versions/recent?vendorId=${vendorId}&years=${years}`;
    return axios.get<IDatabaseVendorVersion[]>(requestUrl);
  },
  { serializeError: serializeAxiosError },
);

export const DatabaseVendorVersionSlice = createSlice({
  name: 'databaseVendorVersion',
  initialState: initialState as DatabaseVendorVersionState,
  reducers: {
    reset: () => initialState,
  },
  extraReducers(builder) {
    builder
      .addCase(getVendorVersions.pending, state => {
        state.loading = true;
        state.errorMessage = null;
      })
      .addCase(getVendorVersions.fulfilled, (state, action) => {
        state.loading = false;
        state.versions = action.payload.data;
      })
      .addCase(getVendorVersions.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message || null;
      })
      .addCase(getRecentVendorVersions.pending, state => {
        state.loading = true;
        state.errorMessage = null;
      })
      .addCase(getRecentVendorVersions.fulfilled, (state, action) => {
        state.loading = false;
        state.recentVersions = action.payload.data;
      })
      .addCase(getRecentVendorVersions.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message || null;
      });
  },
});

export const { reset } = DatabaseVendorVersionSlice.actions;

// Reducer
export default DatabaseVendorVersionSlice.reducer;
