import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { IDatabaseVendor } from './database-vendor.model';

const initialState = {
  loading: false,
  errorMessage: null,
  databaseVendors: [] as ReadonlyArray<IDatabaseVendor>,
  databaseVendor: null as IDatabaseVendor | null,
  activeDatabaseVendors: [] as ReadonlyArray<IDatabaseVendor>,
};

export type DatabaseVendorManagementState = Readonly<typeof initialState>;

// Actions
export const getDatabaseVendors = createAsyncThunk('databaseVendorManagement/fetch_all', async () => {
  const requestUrl = 'api/database-vendors';
  return axios.get<IDatabaseVendor[]>(requestUrl);
});

export const getDatabaseVendor = createAsyncThunk('databaseVendorManagement/fetch_one', async (id: string | number) => {
  const requestUrl = `api/database-vendors/${id}`;
  return axios.get<IDatabaseVendor>(requestUrl);
});

export const createDatabaseVendor = createAsyncThunk('databaseVendorManagement/create', async (vendor: IDatabaseVendor) => {
  const result = await axios.post<IDatabaseVendor>('api/database-vendors', vendor);
  return result;
});

export const updateDatabaseVendor = createAsyncThunk('databaseVendorManagement/update', async (vendor: IDatabaseVendor) => {
  const result = await axios.put<IDatabaseVendor>(`api/database-vendors/${vendor.id}`, vendor);
  return result;
});

export const deleteDatabaseVendor = createAsyncThunk('databaseVendorManagement/delete', async (id: string | number) => {
  const requestUrl = `api/database-vendors/${id}`;
  await axios.delete(requestUrl);
  return id;
});

export const DatabaseVendorManagementSlice = createSlice({
  name: 'databaseVendorManagement',
  initialState: initialState as DatabaseVendorManagementState,
  reducers: {
    reset() {
      return initialState;
    },
  },
  extraReducers(builder) {
    builder
      .addCase(getDatabaseVendors.pending, state => {
        state.loading = true;
      })
      .addCase(getDatabaseVendors.fulfilled, (state, action) => {
        state.loading = false;
        const vendors = action.payload.data;
        state.databaseVendors = vendors;
        state.activeDatabaseVendors = vendors.filter(v => v.active !== false);
      })
      .addCase(getDatabaseVendors.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getDatabaseVendor.pending, state => {
        state.loading = true;
      })
      .addCase(getDatabaseVendor.fulfilled, (state, action) => {
        state.loading = false;
        state.databaseVendor = action.payload.data;
      })
      .addCase(getDatabaseVendor.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(createDatabaseVendor.fulfilled, (state, action) => {
        state.loading = false;
        const vendor = action.payload.data;
        state.databaseVendors = [...state.databaseVendors, vendor];
        if (vendor.active !== false) {
          state.activeDatabaseVendors = [...state.activeDatabaseVendors, vendor];
        }
      })
      .addCase(updateDatabaseVendor.fulfilled, (state, action) => {
        state.loading = false;
        const vendor = action.payload.data;
        const index = state.databaseVendors.findIndex(v => v.id === vendor.id);
        if (index >= 0) {
          state.databaseVendors[index] = vendor;
        }
        if (state.databaseVendor?.id === vendor.id) {
          state.databaseVendor = vendor;
        }
        if (vendor.active !== false) {
          const activeIndex = state.activeDatabaseVendors.findIndex(v => v.id === vendor.id);
          if (activeIndex >= 0) {
            state.activeDatabaseVendors[activeIndex] = vendor;
          } else {
            state.activeDatabaseVendors = [...state.activeDatabaseVendors, vendor];
          }
        } else {
          state.activeDatabaseVendors = state.activeDatabaseVendors.filter(v => v.id !== vendor.id);
        }
      })
      .addCase(deleteDatabaseVendor.fulfilled, (state, action) => {
        state.loading = false;
        const id = action.payload;
        state.databaseVendors = state.databaseVendors.filter(v => v.id !== id);
        state.activeDatabaseVendors = state.activeDatabaseVendors.filter(v => v.id !== id);
        if (state.databaseVendor?.id === id) {
          state.databaseVendor = null;
        }
      });
  },
});

export const { reset } = DatabaseVendorManagementSlice.actions;

export default DatabaseVendorManagementSlice.reducer;
